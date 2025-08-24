package rc.maces.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.MaceManager;

import java.util.Collection;

public class PassiveEffectsListener extends BukkitRunnable {

    private final MaceManager maceManager;

    public PassiveEffectsListener(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            // Check if player has any custom mace in either hand
            boolean hasFireMace = maceManager.isFireMace(mainHand) || maceManager.isFireMace(offHand);
            boolean hasWaterMace = maceManager.isWaterMace(mainHand) || maceManager.isWaterMace(offHand);
            boolean hasEarthMace = maceManager.isEarthMace(mainHand) || maceManager.isEarthMace(offHand);

            if (hasFireMace) {
                applyFireMacePassives(player);
            }

            if (hasWaterMace) {
                applyWaterMacePassives(player);
            }

            if (hasEarthMace) {
                applyEarthMacePassives(player);
            }
        }
    }

    private void applyFireMacePassives(Player player) {
        // Fire resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false));
    }

    private void applyWaterMacePassives(Player player) {
        // Conduit power
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false));

        // 5x faster swimming (custom water speed boost)
        if (player.isInWater()) {
            // Apply a very strong speed effect when in water
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 4, false, false));
        }

        // Drown nearby players in 4x4 area
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
        for (Entity entity : nearby) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;

                // Check if target is in water
                if (target.getLocation().getBlock().getType() == Material.WATER ||
                        target.getEyeLocation().getBlock().getType() == Material.WATER) {

                    // Remove water breathing and reduce air
                    target.removePotionEffect(PotionEffectType.WATER_BREATHING);
                    if (target.getRemainingAir() > 0) {
                        target.setRemainingAir(Math.max(0, target.getRemainingAir() - 40)); // Faster drowning
                    }
                }
            }
        }
    }

    private void applyEarthMacePassives(Player player) {
        // Haste 5 (level 4 in code = Haste 5 in game)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 4, false, false));

        // Check for suffocation immunity
        if (player.getLocation().getBlock().getType().isSolid()) {
            // Teleport player to safe location above
            for (int y = 1; y <= 10; y++) {
                if (!player.getLocation().clone().add(0, y, 0).getBlock().getType().isSolid()) {
                    player.teleport(player.getLocation().clone().add(0, y, 0));
                    break;
                }
            }
        }
    }
}