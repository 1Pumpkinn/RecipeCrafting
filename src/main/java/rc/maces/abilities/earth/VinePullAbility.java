package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// REWORKED Vine Pull Ability - Now traps entities in place instead of pulling them (no more buggy movement)
public class VinePullAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    public VinePullAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("vine_pull", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        Map<LivingEntity, Location> trappedEntities = new HashMap<>();
        Map<Player, Float> originalWalkSpeeds = new HashMap<>();
        Map<Player, Float> originalFlySpeeds = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    // Properly restore movement and clear all effects
                    restoreAllEntities();
                    cancel();
                    return;
                }

                // Initial trap setup - find and trap all living entities
                if (ticks == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3, 3, 3);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Check trust system - don't trap trusted players
                            if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                                continue;
                            }

                            // Store the target's initial location for complete immobilization
                            Location trapLocation = target.getLocation().clone();
                            trappedEntities.put(target, trapLocation);

                            // For players - completely disable movement
                            if (target instanceof Player) {
                                Player targetPlayer = (Player) target;

                                // Store original speeds only on first application
                                originalWalkSpeeds.put(targetPlayer, targetPlayer.getWalkSpeed());
                                originalFlySpeeds.put(targetPlayer, targetPlayer.getFlySpeed());

                                // Completely prevent movement by setting speeds to 0
                                targetPlayer.setWalkSpeed(0.0f);
                                targetPlayer.setFlySpeed(0.0f);
                            }

                            // Stop any current velocity immediately
                            target.setVelocity(new Vector(0, 0, 0));

                            // Add vine particles around target instead of placing blocks
                            Location targetLoc = target.getLocation();
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    Location particleLoc = targetLoc.clone().add(x, 0, z);
                                    targetLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, Material.VINE.createBlockData());
                                }
                            }

                            // Send message only to players
                            if (target instanceof Player) {
                                ((Player) target).sendMessage(Component.text("🌿 Completely trapped by vines! You cannot move!")
                                        .color(NamedTextColor.DARK_GREEN));
                            }
                        }
                    }
                }

                // Every tick: Force trapped entities to stay exactly in their trap position
                for (Map.Entry<LivingEntity, Location> entry : trappedEntities.entrySet()) {
                    LivingEntity entity = entry.getKey();
                    Location trapLocation = entry.getValue();

                    // Only process if entity is still alive and valid
                    if (!entity.isDead() && entity.isValid()) {
                        // Force entity to exact trap location to prevent any movement or screen jitter
                        entity.teleport(trapLocation);
                        // Cancel any velocity to prevent movement
                        entity.setVelocity(new Vector(0, 0, 0));
                    }
                }

                // Every 20 ticks (1 second): refresh particle effects and send reminder messages
                if (ticks % 20 == 0) {
                    for (Map.Entry<LivingEntity, Location> entry : trappedEntities.entrySet()) {
                        LivingEntity entity = entry.getKey();
                        Location trapLocation = entry.getValue();

                        if (!entity.isDead() && entity.isValid()) {
                            // Refresh vine particles
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    Location particleLoc = trapLocation.clone().add(x, 0, z);
                                    trapLocation.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, Material.VINE.createBlockData());
                                }
                            }
                        }
                    }
                }

                ticks++;
            }

            private void restoreAllEntities() {
                // Clear all trapped entities and restore their states
                for (LivingEntity entity : trappedEntities.keySet()) {
                    if (entity != null && !entity.isDead() && entity.isValid()) {
                        // Restore player movement speeds
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player) entity;

                            // Restore original speeds or use defaults
                            float originalWalkSpeed = originalWalkSpeeds.getOrDefault(targetPlayer, 0.2f);
                            float originalFlySpeed = originalFlySpeeds.getOrDefault(targetPlayer, 0.1f);

                            targetPlayer.setWalkSpeed(originalWalkSpeed);
                            targetPlayer.setFlySpeed(originalFlySpeed);

                            // Send restoration message
                            targetPlayer.sendMessage(Component.text("🌿 You break free from the vine trap!")
                                    .color(NamedTextColor.GREEN));
                        }

                        // Clear any remaining velocity
                        entity.setVelocity(new Vector(0, 0, 0));
                    }
                }

                // Clear all tracking maps
                trappedEntities.clear();
                originalWalkSpeeds.clear();
                originalFlySpeeds.clear();
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                // Ensure restoration happens even if task is cancelled early
                restoreAllEntities();
                super.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("🌿 VINE TRAP! Completely immobilizing enemies for 5 seconds!")
                .color(NamedTextColor.GREEN));
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_SPIDER_STEP, 1.5f, 0.8f);

        setCooldown(player);
    }
}