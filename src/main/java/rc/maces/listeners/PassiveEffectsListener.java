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
import org.bukkit.util.Vector;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

import java.util.Collection;

public class PassiveEffectsListener extends BukkitRunnable {

    private final MaceManager maceManager;
    private final ElementManager elementManager;

    public PassiveEffectsListener(MaceManager maceManager, ElementManager elementManager) {
        this.maceManager = maceManager;
        this.elementManager = elementManager;
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
            boolean hasAirMace = maceManager.isAirMace(mainHand) || maceManager.isAirMace(offHand);

            // Get player's element for role-based passives
            String playerElement = elementManager.getPlayerElement(player);

            if (hasFireMace || "FIRE".equals(playerElement)) {
                applyFireMacePassives(player, hasFireMace);
            }

            if (hasWaterMace || "WATER".equals(playerElement)) {
                applyWaterMacePassives(player, hasWaterMace);
            }

            if (hasEarthMace || "EARTH".equals(playerElement)) {
                applyEarthMacePassives(player, hasEarthMace);
            }

            if (hasAirMace || "AIR".equals(playerElement)) {
                applyAirMacePassives(player, hasAirMace);
            }
        }
    }

    private void applyFireMacePassives(Player player, boolean holdingMace) {
        // Fire immunity (always when holding mace)
        if (holdingMace) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false));
        }

        // When on fire, gain +2 attack damage (works with element role too)
        if (player.getFireTicks() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
        }
    }

    private void applyWaterMacePassives(Player player, boolean holdingMace) {
        if (!holdingMace) return; // Water effects only when holding mace

        // Conduit power
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false));

        // FIXED: 5x faster swimming - use Dolphins Grace for swimming speed
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 4, false, false));
        }

        // Drown nearby living entities (including mobs) in 4x4 area
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check if target is in water
                if (target.getLocation().getBlock().getType() == Material.WATER ||
                        target.getEyeLocation().getBlock().getType() == Material.WATER) {

                    // Handle drowning differently for players vs mobs
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        // Remove water breathing and reduce air for players
                        targetPlayer.removePotionEffect(PotionEffectType.WATER_BREATHING);
                        if (targetPlayer.getRemainingAir() > 0) {
                            targetPlayer.setRemainingAir(Math.max(0, targetPlayer.getRemainingAir() - 40));
                        }
                    } else {
                        // For mobs, simulate drowning by reducing air directly
                        if (target.getMaximumAir() > 0) {
                            int currentAir = target.getRemainingAir();
                            if (currentAir > 0) {
                                target.setRemainingAir(Math.max(0, currentAir - 60));
                            } else {
                                target.damage(1.0);
                            }
                        } else {
                            // For mobs that don't naturally drown
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, false));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                        }
                    }
                }
            }
        }
    }

    private void applyEarthMacePassives(Player player, boolean holdingMace) {
        // Haste 5 when holding mace
        if (holdingMace) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 4, false, false));
        }

        // Suffocation immunity when holding mace
        if (holdingMace && player.getLocation().getBlock().getType().isSolid()) {
            for (int y = 1; y <= 10; y++) {
                if (!player.getLocation().clone().add(0, y, 0).getBlock().getType().isSolid()) {
                    player.teleport(player.getLocation().clone().add(0, y, 0));
                    break;
                }
            }
        }
    }

    private void applyAirMacePassives(Player player, boolean holdingMace) {
        // Air passives are mainly handled in MaceListener (fall damage immunity, etc.)
        // Wind charge pulling is handled there too
    }
}