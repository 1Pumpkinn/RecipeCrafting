package rc.maces.abilities.fire;

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

// Obsidian Creation Ability - UPDATED: Only replaces tall grass and short grass
public class ObsidianCreationAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    public ObsidianCreationAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("obsidian_creation", 30, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        Map<Location, Material> originalBlocks = new HashMap<>();
        int blocksConverted = 0;

        // Find all entities in 8 block radius and spawn obsidian on them (except allies)
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 4, 4, 4); // 8 block range
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check trust system - don't trap trusted players (NO SPAM MESSAGE)
                if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                    continue;
                }

                Location targetLoc = target.getLocation();

                // Spawn obsidian in a larger box (4x5x4) but ONLY replace grass types
                for (int x = -2; x <= 1; x++) { // 4 blocks wide
                    for (int z = -2; z <= 1; z++) { // 4 blocks deep
                        for (int y = 0; y <= 4; y++) { // Height of 5 blocks
                            Location obsidianLoc = targetLoc.clone().add(x, y, z);
                            Material blockType = obsidianLoc.getBlock().getType();

                            // UPDATED: Only replace tall grass and short grass (and air for completion)
                            if (blockType == Material.TALL_GRASS ||
                                    blockType == Material.SHORT_GRASS ||
                                    blockType == Material.AIR) {

                                // Don't replace existing obsidian
                                if (blockType != Material.OBSIDIAN) {
                                    originalBlocks.put(obsidianLoc.clone(), blockType);
                                    obsidianLoc.getBlock().setType(Material.OBSIDIAN);
                                    blocksConverted++;

                                    // Enhanced visual effects at obsidian creation sites
                                    obsidianLoc.getWorld().spawnParticle(Particle.SMOKE, obsidianLoc.add(0.5, 0.5, 0.5), 15);
                                    obsidianLoc.getWorld().spawnParticle(Particle.LAVA, obsidianLoc, 8);
                                    obsidianLoc.getWorld().spawnParticle(Particle.FLAME, obsidianLoc, 12);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (blocksConverted == 0) {
            player.sendMessage(Component.text("🖤 No entities found to trap in obsidian!")
                    .color(NamedTextColor.GRAY));
            return; // Don't set cooldown if no obsidian was created
        }

        center.getWorld().playSound(center, Sound.BLOCK_LAVA_POP, 2.0f, 0.4f);
        center.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 0.6f);

        // Damage entities that were trapped and continue damaging
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 300) { // 15 seconds of damage dealing
                    cancel();
                    return;
                }

                // Deal damage to entities standing on obsidian blocks (that were converted) every 1.5 seconds
                if (ticks % 30 == 0) {
                    // Check in 8 block radius (same as initial range)
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 4, 4, 4);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Check trust system - don't damage trusted players (NO SPAM MESSAGE)
                            if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                                continue;
                            }

                            Location targetLoc = target.getLocation();

                            // Check if entity is standing on converted obsidian
                            Location blockBelow = targetLoc.clone().add(0, -1, 0);
                            Location blockAt = targetLoc.getBlock().getLocation();

                            boolean isOnConvertedObsidian = false;

                            // Check if standing on or in a converted obsidian block
                            if (originalBlocks.containsKey(blockBelow) && blockBelow.getBlock().getType() == Material.OBSIDIAN) {
                                isOnConvertedObsidian = true;
                            }
                            if (originalBlocks.containsKey(blockAt) && blockAt.getBlock().getType() == Material.OBSIDIAN) {
                                isOnConvertedObsidian = true;
                            }

                            if (isOnConvertedObsidian) {
                                // Deal 2 hearts (4 damage) true damage + ignite
                                double newHealth = Math.max(0, target.getHealth() - 4.0);
                                target.setHealth(newHealth);
                                target.setFireTicks(60); // Ignite for 3 seconds

                                // Add knockback effect
                                Vector knockback = targetLoc.toVector().subtract(center.toVector()).normalize().multiply(1.5);
                                knockback.setY(0.8); // Add upward component
                                target.setVelocity(knockback);

                                // Enhanced visual effects
                                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 20);
                                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 15);
                                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 25);
                                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_LAVA_POP, 1.5f, 1.2f);
                                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Schedule obsidian removal after 1 minutes (1200 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Remove all obsidian blocks that were converted
                for (Location obsidianLoc : originalBlocks.keySet()) {
                    if (obsidianLoc.getBlock().getType() == Material.OBSIDIAN) {
                        Material originalType = originalBlocks.get(obsidianLoc);
                        obsidianLoc.getBlock().setType(originalType);
                    }
                }
            }
        }.runTaskLater(plugin, 1200L); // 1 minutes = 1200 ticks

        setCooldown(player);
    }
}