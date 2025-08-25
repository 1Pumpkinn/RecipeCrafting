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
import java.util.UUID;

// FIXED Vine Pull Ability - Now completely immobilizes entities without screen jitter
public class VinePullAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    // Static tracking to prevent movement across all instances
    private static final Map<UUID, VineTrappedData> trappedEntities = new HashMap<>();
    private static JavaPlugin pluginInstance;

    // Helper class to store trapped entity data
    private static class VineTrappedData {
        final Location trapLocation;
        final float originalWalkSpeed;
        final float originalFlySpeed;
        final long trapEndTime;

        VineTrappedData(Location location, float walkSpeed, float flySpeed, long endTime) {
            this.trapLocation = location.clone();
            this.originalWalkSpeed = walkSpeed;
            this.originalFlySpeed = flySpeed;
            this.trapEndTime = endTime;
        }
    }

    public VinePullAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("vine_pull", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;

        // Set the static plugin instance
        VinePullAbility.pluginInstance = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        int trappedCount = 0;
        long trapEndTime = System.currentTimeMillis() + 5000; // 5 seconds from now

        // Find and trap all living entities in range
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3, 3, 3);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check trust system - don't trap trusted players
                if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                    continue;
                }

                // Store trap data for this entity
                float originalWalkSpeed = 0.2f;
                float originalFlySpeed = 0.1f;

                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    originalWalkSpeed = targetPlayer.getWalkSpeed();
                    originalFlySpeed = targetPlayer.getFlySpeed();

                    // Completely disable movement
                    targetPlayer.setWalkSpeed(0.0f);
                    targetPlayer.setFlySpeed(0.0f);
                }

                Location trapLocation = target.getLocation().clone();
                VineTrappedData trapData = new VineTrappedData(trapLocation, originalWalkSpeed, originalFlySpeed, trapEndTime);
                trappedEntities.put(target.getUniqueId(), trapData);

                // Stop any current movement
                target.setVelocity(new Vector(0, 0, 0));

                trappedCount++;

                // Send message only to players
                if (target instanceof Player) {
                    ((Player) target).sendMessage(Component.text("🌿 Completely trapped by vines! You cannot move for 5 seconds!")
                            .color(NamedTextColor.DARK_GREEN));
                }
            }
        }

        if (trappedCount == 0) {
            player.sendMessage(Component.text("🌿 No entities found to trap!")
                    .color(NamedTextColor.GRAY));
            return;
        }

        // Start the trap effect task
        startTrapEffectTask(center, trapEndTime);

        player.sendMessage(Component.text("🌿 VINE TRAP! Completely immobilized " + trappedCount + " entities for 5 seconds!")
                .color(NamedTextColor.GREEN));
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_SPIDER_STEP, 1.5f, 0.8f);

        setCooldown(player);
    }

    private void startTrapEffectTask(Location center, long trapEndTime) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                if (currentTime >= trapEndTime) {
                    // Release all trapped entities
                    releaseAllTrappedEntities();
                    cancel();
                    return;
                }

                // Maintain traps and create visual effects
                for (Map.Entry<UUID, VineTrappedData> entry : trappedEntities.entrySet()) {
                    UUID entityId = entry.getKey();
                    VineTrappedData trapData = entry.getValue();

                    if (trapData.trapEndTime <= currentTime) {
                        continue; // This entity should be released
                    }

                    Entity entity = null;
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        if (p.getUniqueId().equals(entityId)) {
                            entity = p;
                            break;
                        }
                    }

                    if (entity == null) {
                        // Try to find among all entities in the world
                        if (pluginInstance != null) {
                            for (Entity e : center.getWorld().getEntities()) {
                                if (e.getUniqueId().equals(entityId)) {
                                    entity = e;
                                    break;
                                }
                            }
                        }
                    }

                    if (entity != null && entity.isValid() && !entity.isDead()) {
                        // Force entity to stay in trap location (without the jittery teleporting)
                        Location currentLoc = entity.getLocation();
                        Location trapLoc = trapData.trapLocation;

                        // Only teleport if they've moved significantly (reduces jitter)
                        if (currentLoc.distance(trapLoc) > 0.1) {
                            entity.teleport(trapLoc);
                        }

                        // Continuously cancel any movement
                        entity.setVelocity(new Vector(0, 0, 0));

                        // Create vine particles every second
                        if (System.currentTimeMillis() % 1000 < 50) { // Roughly every second
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    Location particleLoc = trapLoc.clone().add(x, 0.5, z);
                                    trapLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, Material.VINE.createBlockData());
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks for smoother effect
    }

    private void releaseAllTrappedEntities() {
        for (Map.Entry<UUID, VineTrappedData> entry : trappedEntities.entrySet()) {
            UUID entityId = entry.getKey();
            VineTrappedData trapData = entry.getValue();

            Entity entity = null;
            if (pluginInstance != null) {
                for (Player p : pluginInstance.getServer().getOnlinePlayers()) {
                    if (p.getUniqueId().equals(entityId)) {
                        entity = p;
                        break;
                    }
                }
            }

            if (entity instanceof Player) {
                Player player = (Player) entity;

                // Restore original movement speeds
                player.setWalkSpeed(trapData.originalWalkSpeed);
                player.setFlySpeed(trapData.originalFlySpeed);

                // Clear any remaining velocity
                player.setVelocity(new Vector(0, 0, 0));

                // Send release message
                player.sendMessage(Component.text("🌿 You break free from the vine trap!")
                        .color(NamedTextColor.GREEN));
            }
        }

        trappedEntities.clear();
    }

    // Static method to check if an entity is trapped (for use in movement events)
    public static boolean isEntityTrapped(UUID entityId) {
        VineTrappedData trapData = trappedEntities.get(entityId);
        if (trapData == null) {
            return false;
        }

        if (System.currentTimeMillis() >= trapData.trapEndTime) {
            trappedEntities.remove(entityId);
            return false;
        }

        return true;
    }

    // Static method to get trap location for an entity
    public static Location getTrapLocation(UUID entityId) {
        VineTrappedData trapData = trappedEntities.get(entityId);
        return trapData != null ? trapData.trapLocation.clone() : null;
    }

    // Static method to force release a specific entity (if needed)
    public static void releaseEntity(UUID entityId) {
        VineTrappedData trapData = trappedEntities.remove(entityId);
        if (trapData != null) {
            Entity entity = null;
            if (pluginInstance != null) {
                for (Player p : pluginInstance.getServer().getOnlinePlayers()) {
                    if (p.getUniqueId().equals(entityId)) {
                        entity = p;
                        break;
                    }
                }
            }

            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.setWalkSpeed(trapData.originalWalkSpeed);
                player.setFlySpeed(trapData.originalFlySpeed);
                player.setVelocity(new Vector(0, 0, 0));
            }
        }
    }
}