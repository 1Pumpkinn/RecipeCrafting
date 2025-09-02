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
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;
import rc.maces.managers.TrustManager;

import java.util.*;

public class PassiveEffectsListener extends BukkitRunnable {

    private final MaceManager maceManager;
    private final ElementManager elementManager;
    private final TrustManager trustManager;

    public PassiveEffectsListener(MaceManager maceManager, ElementManager elementManager, TrustManager trustManager) {
        this.maceManager = maceManager;
        this.elementManager = elementManager;
        this.trustManager = trustManager;
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
        // Fire immunity (always when holding mace or has fire element)
        if (holdingMace || "FIRE".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false));
        }

        // When on fire, gain +1 attack damage (reduced from +2)
        if (player.getFireTicks() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
        }
    }

    private void applyWaterMacePassives(Player player, boolean holdingMace) {
        // Water effects when holding mace or has water element
        if (holdingMace || "WATER".equals(elementManager.getPlayerElement(player))) {
            // Conduit power
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false));

            // UPDATED: Dolphins Grace level 3 when holding mace, level 1 when just has water element
            if (player.isInWater()) {
                if (holdingMace) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 2, false, false)); // Level 3
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false)); // Level 1
                }
            }
        }
    }

    private void applyEarthMacePassives(Player player, boolean holdingMace) {
        // UPDATED: Haste 5 when holding mace, Haste 3 when just has earth element
        if (holdingMace) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 4, false, false)); // Haste 5
        } else if ("EARTH".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 2, false, false)); // Haste 3
        }

        // Hero of the Village level 1 when holding mace or has earth element
        if (holdingMace || "EARTH".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 40, 0, false, false));
        }
    }

    private void applyAirMacePassives(Player player, boolean holdingMace) {
        // Speed 1 when holding mace or has air element
        if (holdingMace || "AIR".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false));
        }
    }
}