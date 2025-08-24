package rc.maces.abilities.fire;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

// Obsidian Creation Ability - Turns water/lava into permanent obsidian in 4x4 area
public class ObsidianCreationAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public ObsidianCreationAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("obsidian_creation", 15, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        int blocksConverted = 0;

        // Convert water to obsidian in 8x8 area around player
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -1; y <= 2; y++) { // Check multiple Y levels
                    Location checkLoc = center.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();

                    if (blockType == Material.WATER) {
                        checkLoc.getBlock().setType(Material.OBSIDIAN);
                        blocksConverted++;

                        // Visual effects
                        checkLoc.getWorld().spawnParticle(Particle.FLAME, checkLoc, 10);
                        checkLoc.getWorld().spawnParticle(Particle.LAVA, checkLoc, 5);
                        checkLoc.getWorld().spawnParticle(Particle.SMOKE, checkLoc, 8);
                    }
                }
            }
        }

        // If player is jumping/airborne, also convert water beneath them in a larger area
        if (!player.isOnGround()) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    for (int y = -5; y <= -1; y++) { // Check deeper below when jumping
                        Location checkLoc = center.clone().add(x, y, z);
                        Material blockType = checkLoc.getBlock().getType();

                        if (blockType == Material.WATER) {
                            checkLoc.getBlock().setType(Material.OBSIDIAN);
                            blocksConverted++;

                            // Visual effects for below conversion
                            checkLoc.getWorld().spawnParticle(Particle.FLAME, checkLoc, 8);
                            checkLoc.getWorld().spawnParticle(Particle.LAVA, checkLoc, 4);
                            checkLoc.getWorld().spawnParticle(Particle.SMOKE, checkLoc, 6);
                        }
                    }
                }
            }
        }

        if (blocksConverted > 0) {
            player.sendMessage(Component.text("🔥 Converted " + blocksConverted + "blocks")
                    .color(NamedTextColor.DARK_PURPLE));
            center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 0.5f);
            center.getWorld().playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1.5f, 0.8f);
        } else {
            player.sendMessage(Component.text("🔥 No water found to convert!")
                    .color(NamedTextColor.YELLOW));
        }

        setCooldown(player);
    }
}