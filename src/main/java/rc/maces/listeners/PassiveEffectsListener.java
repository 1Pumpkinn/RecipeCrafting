package rc.maces.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

            if (maceManager.isFireMace(mainHand)) {
                // Fire resistance for Fire Mace holders
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false));

            } else if (maceManager.isWaterMace(mainHand)) {
                // Conduit Power and Dolphin's Grace for Water Mace holders
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false));

                // Drown nearby enemies (apply water breathing removal effect)
                Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 4, 4, 4);
                for (Entity entity : nearby) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity living = (LivingEntity) entity;

                        // Check if entity is underwater or in water
                        if (living.getLocation().getBlock().getType() == Material.WATER ||
                                living.getEyeLocation().getBlock().getType() == Material.WATER) {

                            // Remove water breathing and cause drowning damage
                            living.removePotionEffect(PotionEffectType.WATER_BREATHING);
                            if (living.getRemainingAir() > 0) {
                                living.setRemainingAir(Math.max(0, living.getRemainingAir() - 20));
                            }
                        }
                    }
                }

            } else if (maceManager.isEarthMace(mainHand)) {
                // Stone Skin effect (resistance) for Earth Mace holders
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1, false, false));

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
    }
}