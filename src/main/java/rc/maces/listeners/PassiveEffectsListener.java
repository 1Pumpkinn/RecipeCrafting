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

    // Track entities that were previously drowning to know when they escape
    private final Map<UUID, Set<UUID>> previouslyDrowningEntities = new HashMap<>();

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

        // When on fire, gain +2 attack damage (works with element role too)
        if (player.getFireTicks() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
        }
    }

    private void applyWaterMacePassives(Player player, boolean holdingMace) {
        // Water effects when holding mace or has water element
        if (holdingMace || "WATER".equals(elementManager.getPlayerElement(player))) {
            // Conduit power
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false));

            // 5x faster swimming - use Dolphins Grace for swimming speed
            if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 4, false, false));
            }
        }

        // FIXED drowning effect for nearby living entities in 4x4 area
        if (holdingMace || "WATER".equals(elementManager.getPlayerElement(player))) {
            Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2);
            Set<UUID> currentlyDrowningEntities = new HashSet<>();

            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;

                    // Check trust system - don't affect trusted players
                    if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                        continue;
                    }

                    currentlyDrowningEntities.add(target.getUniqueId());

                    // Apply drowning effect - reduce air and damage when air runs out
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        // Remove any water breathing effect first
                        targetPlayer.removePotionEffect(PotionEffectType.WATER_BREATHING);
                        if (targetPlayer.getRemainingAir() > 0) {
                            targetPlayer.setRemainingAir(Math.max(0, targetPlayer.getRemainingAir() - 60));
                        } else {
                            targetPlayer.damage(2.0); // Drowning damage
                        }
                    } else {
                        // For mobs - reduce air and damage
                        if (target.getRemainingAir() > 0) {
                            target.setRemainingAir(Math.max(0, target.getRemainingAir() - 80));
                        } else {
                            target.damage(2.0); // Drowning damage
                        }
                    }
                }
            }

            // Get previously drowning entities for this water player
            Set<UUID> previouslyDrowning = previouslyDrowningEntities.getOrDefault(player.getUniqueId(), new HashSet<>());

            // Find entities that are no longer in range (escaped drowning area)
            Set<UUID> escapedEntities = new HashSet<>(previouslyDrowning);
            escapedEntities.removeAll(currentlyDrowningEntities);

            // Restore air for entities that escaped the drowning area
            for (UUID escapedUUID : escapedEntities) {
                Entity escapedEntity = player.getWorld().getEntity(escapedUUID);
                if (escapedEntity instanceof LivingEntity) {
                    LivingEntity escapedLiving = (LivingEntity) escapedEntity;
                    // Gradually restore air when out of drowning range
                    int currentAir = escapedLiving.getRemainingAir();
                    int maxAir = escapedLiving.getMaximumAir();
                    if (currentAir < maxAir) {
                        escapedLiving.setRemainingAir(Math.min(maxAir, currentAir + 100));
                    }
                }
            }

            // Update the tracking set
            previouslyDrowningEntities.put(player.getUniqueId(), currentlyDrowningEntities);
        }
    }

    private void applyEarthMacePassives(Player player, boolean holdingMace) {
        // Haste 5 when holding mace or has earth element
        if (holdingMace || "EARTH".equals(elementManager.getPlayerElement(player))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 4, false, false));
        }

        // Hero of the Village effect when holding mace or has earth element
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