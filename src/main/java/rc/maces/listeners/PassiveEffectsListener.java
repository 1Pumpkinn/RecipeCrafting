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

    // Track entities that are currently in drowning areas and their air drain progress
    private final Map<UUID, Map<UUID, DrownData>> playerDrowningTargets = new HashMap<>();

    // Helper class to track drowning progress
    private static class DrownData {
        long firstEntered;
        int airDrainLevel;
        long lastDamage;

        DrownData() {
            this.firstEntered = System.currentTimeMillis();
            this.airDrainLevel = 0;
            this.lastDamage = 0;
        }
    }

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

            // Dolphins Grace level 1 (reduced from level 4)
            if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false));
            }
        }

        // FIXED drowning effect for nearby living entities in 4x4 area
        if (holdingMace || "WATER".equals(elementManager.getPlayerElement(player))) {
            Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
            Set<UUID> currentlyInRange = new HashSet<>();

            // Get or create the drowning targets map for this water player
            Map<UUID, DrownData> drownTargets = playerDrowningTargets.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

            long currentTime = System.currentTimeMillis();

            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;

                    // Check trust system - don't affect trusted players
                    if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                        continue;
                    }

                    UUID targetId = target.getUniqueId();
                    currentlyInRange.add(targetId);

                    // Get or create drown data for this target
                    DrownData drownData = drownTargets.computeIfAbsent(targetId, k -> new DrownData());

                    // Only start drowning process after being in range for 2 seconds (grace period)
                    long timeInRange = currentTime - drownData.firstEntered;
                    if (timeInRange < 2000) {
                        continue; // Still in grace period
                    }

                    // Progressive air drain - start slow, get faster
                    int targetAirLevel;
                    if (timeInRange < 4000) { // 2-4 seconds: slow drain
                        targetAirLevel = 200; // About 2/3 air
                    } else if (timeInRange < 6000) { // 4-6 seconds: medium drain
                        targetAirLevel = 100; // About 1/3 air
                    } else { // 6+ seconds: full drowning
                        targetAirLevel = 0; // No air, taking damage
                    }

                    // Apply air level
                    target.setRemainingAir(targetAirLevel);

                    // Only deal damage when air is 0 and enough time has passed since last damage
                    if (targetAirLevel == 0 && (currentTime - drownData.lastDamage) >= 1000) {
                        target.damage(2.0);
                        drownData.lastDamage = currentTime;
                    }
                }
            }

            // Handle entities that left the drowning area
            Set<UUID> entitiesWhoLeft = new HashSet<>(drownTargets.keySet());
            entitiesWhoLeft.removeAll(currentlyInRange);

            // Remove entities that left and restore their air gradually
            for (UUID leftEntityId : entitiesWhoLeft) {
                Entity leftEntity = player.getWorld().getEntity(leftEntityId);
                if (leftEntity instanceof LivingEntity) {
                    LivingEntity leftTarget = (LivingEntity) leftEntity;
                    // Restore air gradually when they escape
                    int currentAir = leftTarget.getRemainingAir();
                    int maxAir = leftTarget.getMaximumAir();
                    if (currentAir < maxAir) {
                        leftTarget.setRemainingAir(Math.min(maxAir, currentAir + 50)); // Gradual restoration
                    }
                }
                drownTargets.remove(leftEntityId);
            }
        } else {
            // If player doesn't have water abilities anymore, clear their drowning targets
            playerDrowningTargets.remove(player.getUniqueId());
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
        // Speed 1 when holding mace or has air element
        if (holdingMace || "AIR".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false));
        }

        // Air passives are mainly handled in MaceListener (fall damage immunity, etc.)
        // Wind charge pulling is handled there too
    }
}