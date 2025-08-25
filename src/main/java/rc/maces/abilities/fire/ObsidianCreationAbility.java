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
        int waterConverted = 0;

        // Find all entities in the area and spawn obsidian on them (except allies)
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 4, 4, 4);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check trust system - don't trap trusted players
                if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                    continue;
                }

                Location targetLoc = target.getLocation();

                // Spawn obsidian at the entity's location and around them
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int y = 0; y <= 2; y++) {
                            Location obsidianLoc = targetLoc.clone().add(x, y, z);
                            Material blockType = obsidianLoc.getBlock().getType();

                            // Only replace air, water, or lava blocks
                            if (blockType == Material.AIR || blockType == Material.WATER || blockType == Material.LAVA) {
                                originalBlocks.put(obsidianLoc.clone(), blockType);
                                obsidianLoc.getBlock().setType(Material.OBSIDIAN);
                                waterConverted++;

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

        if (waterConverted == 0) {
            player.sendMessage(Component.text("🖤 No entities found to trap in obsidian!")
                    .color(NamedTextColor.GRAY));
            return; // Don't set cooldown if no obsidian was created
        }

        player.sendMessage(Component.text("🖤 OBSIDIAN CREATION! Converted " + waterConverted + " water blocks to obsidian!")
                .color(NamedTextColor.DARK_PURPLE));
        center.getWorld().playSound(center, Sound.BLOCK_LAVA_POP, 2.0f, 0.4f);
        center.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 0.6f);

        // Damage entities that were in water when it converted and continue damaging
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 300) { // 15 seconds (increased duration)
                    // Obsidian is now permanent - no restoration
                    cancel();
                    return;
                }

                // Deal damage to entities standing on obsidian blocks (that were water) every 1.5 seconds
                if (ticks % 30 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 5, 5, 5);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Check trust system - don't damage trusted players
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

                                if (target instanceof Player) {
                                    ((Player) target).sendMessage(Component.text("🖤🔥 Burning on converted obsidian! Taking damage and ignited!")
                                            .color(NamedTextColor.DARK_PURPLE));
                                }
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        setCooldown(player);
    }
}