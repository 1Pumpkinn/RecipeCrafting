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
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ObsidianCreationAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public ObsidianCreationAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("obsidian_creation", 30, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        Map<Location, Material> originalBlocks = new HashMap<>();
        int waterConverted = 0;

        // Convert all water blocks in a 7x7x3 area to obsidian
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 1; y++) {
                    Location loc = center.clone().add(x, y, z);
                    Material originalBlock = loc.getBlock().getType();

                    // ONLY convert water blocks to obsidian
                    if (originalBlock == Material.WATER) {
                        originalBlocks.put(loc.clone(), originalBlock);
                        loc.getBlock().setType(Material.OBSIDIAN);
                        waterConverted++;

                        // Visual effects at conversion sites
                        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0.5, 0.5, 0.5), 8);
                        loc.getWorld().spawnParticle(Particle.LAVA, loc, 3);
                    }
                }
            }
        }

        if (waterConverted == 0) {
            player.sendMessage(Component.text("🖤 No water found to convert to obsidian!")
                    .color(NamedTextColor.GRAY));
            return; // Don't set cooldown if no water was found
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
                if (ticks >= 200) { // 10 seconds
                    // Restore original water blocks
                    for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                        Location loc = entry.getKey();
                        Material original = entry.getValue();

                        if (loc.getBlock().getType() == Material.OBSIDIAN) {
                            loc.getBlock().setType(original); // Restore to water
                        }
                    }
                    cancel();
                    return;
                }

                // Deal damage to entities standing on obsidian blocks (that were water) every 2 seconds
                if (ticks % 40 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 4, 4, 4);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
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
                                // Deal 1.5 hearts (3 damage) true damage
                                double newHealth = Math.max(0, target.getHealth() - 3.0);
                                target.setHealth(newHealth);

                                // Visual effects
                                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 12);
                                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 10);
                                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_LAVA_POP, 1.0f, 1.2f);

                                if (target instanceof Player) {
                                    ((Player) target).sendMessage(Component.text("🖤 Burning on converted obsidian! Taking damage!")
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