package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.*;

// UPDATED BuddyUp Ability - Golem persists until 50+ blocks away or new golem summoned
public class BuddyUpAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;
    private static final Map<UUID, GolemInfo> playerGolems = new HashMap<>();
    private static final Set<UUID> golemUUIDs = new HashSet<>();
    private static final double MAX_DISTANCE = 50.0; // Maximum distance before golem despawns

    // Helper class to store golem information
    private static class GolemInfo {
        final IronGolem golem;
        final UUID summonerUUID;

        GolemInfo(IronGolem golem, UUID summonerUUID) {
            this.golem = golem;
            this.summonerUUID = summonerUUID;
        }
    }

    public BuddyUpAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("buddy_up", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        // Remove existing golem if any
        removeExistingGolem(player);

        Location spawnLoc = player.getLocation().add(2, 0, 0);
        IronGolem golem = (IronGolem) player.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);

        // Set health to 50 hearts (100 HP) - ENHANCED!
        golem.setMaxHealth(100.0);
        golem.setHealth(100.0);

        // Give the golem Strength 2 effect (permanent duration) - ENHANCED!
        golem.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));

        // Make it protect the player
        golem.setCustomName("§a" + player.getName() + "'s Buddy");
        golem.setCustomNameVisible(true);

        // Make the golem never naturally aggressive and always friendly to summoner
        golem.setTarget(null);
        golem.setAggressive(false);

        // Prevent natural despawning
        golem.setPersistent(true);

        // Store the golem with summoner info
        GolemInfo golemInfo = new GolemInfo(golem, player.getUniqueId());
        playerGolems.put(player.getUniqueId(), golemInfo);
        golemUUIDs.add(golem.getUniqueId());

        // Sound effect and visual feedback
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.8f);

        // Start continuous monitoring task
        startGolemMonitoring(player, golem);

        setCooldown(player);
    }

    /**
     * Start the continuous monitoring task for a golem
     */
    private void startGolemMonitoring(Player player, IronGolem golem) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Stop monitoring if golem is dead or invalid
                if (golem.isDead() || !golem.isValid()) {
                    cancel();
                    return;
                }

                // Stop monitoring if player is offline
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // If golem is targeting its summoner, clear the target immediately
                if (golem.getTarget() != null && golem.getTarget().getUniqueId().equals(player.getUniqueId())) {
                    golem.setTarget(null);
                    golem.setAggressive(false);
                }

                // Ensure Strength 2 effect is maintained (reapply if missing)
                if (!golem.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    golem.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
                }

                // Check distance from summoner - remove if too far
                if (golem.getLocation().getWorld().equals(player.getLocation().getWorld())) {
                    double distance = golem.getLocation().distance(player.getLocation());

                    if (distance > MAX_DISTANCE) {
                        // Golem is too far away, remove it

                        removeExistingGolem(player);
                        cancel();
                        return;
                    }
                } else {
                    // Different worlds, remove golem
                    removeExistingGolem(player);
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Check every 2 seconds (40 ticks)
    }

    private void removeExistingGolem(Player player) {
        GolemInfo existingGolemInfo = playerGolems.remove(player.getUniqueId());
        if (existingGolemInfo != null && existingGolemInfo.golem != null && !existingGolemInfo.golem.isDead()) {
            golemUUIDs.remove(existingGolemInfo.golem.getUniqueId());

            // Clear all potion effects before removing
            existingGolemInfo.golem.clearActivePotionEffects();
            existingGolemInfo.golem.remove();
        }
    }

    // Handle when the player gets damaged by ANY living entity (checks 8 block range)
    public static void handlePlayerDamage(EntityDamageByEntityEvent event, Player victim, TrustManager trustManager) {
        GolemInfo golemInfo = playerGolems.get(victim.getUniqueId());
        if (golemInfo == null || golemInfo.golem == null || golemInfo.golem.isDead()) {
            return;
        }

        IronGolem golem = golemInfo.golem;

        // Check if golem is within 8 blocks of the victim
        if (golem.getLocation().distance(victim.getLocation()) > 8.0) {
            return; // Golem is too far away to help
        }

        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();

            // Always prevent golem from attacking its own summoner
            if (attacker.getUniqueId().equals(victim.getUniqueId())) {
                return;
            }

            // Don't make golem attack other golems (prevent golem wars)
            if (attacker instanceof IronGolem && golemUUIDs.contains(attacker.getUniqueId())) {
                return;
            }

            // Check trust system - don't attack trusted players
            if (attacker instanceof Player && trustManager.isTrusted(victim, (Player) attacker)) {
                return;
            }

            // Make golem target the attacker
            golem.setTarget(attacker);

            // Make sure golem is aggressive towards the target
            if (attacker instanceof Player) {
                golem.setAggressive(true);
            }
        }
    }

    // Handle when a golem gets damaged - completely prevent retaliation against summoner
    public static void handleGolemDamage(EntityDamageByEntityEvent event, IronGolem golem, TrustManager trustManager) {
        if (!golemUUIDs.contains(golem.getUniqueId())) {
            return; // Not one of our custom golems
        }

        // Find the summoner of this golem
        Player summoner = null;
        for (GolemInfo golemInfo : playerGolems.values()) {
            if (golemInfo.golem != null && golemInfo.golem.equals(golem)) {
                summoner = org.bukkit.Bukkit.getPlayer(golemInfo.summonerUUID);
                break;
            }
        }

        if (summoner == null) {
            return;
        }

        // If the summoner is attacking their own golem, NEVER let the golem retaliate
        if (event.getDamager().getUniqueId().equals(summoner.getUniqueId())) {
            // Immediately clear any target and make golem passive towards summoner
            golem.setTarget(null);
            golem.setAggressive(false);
            return;
        } else if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();

            // Check trust system - don't retaliate against trusted players
            if (attacker instanceof Player && trustManager.isTrusted(summoner, (Player) attacker)) {
                golem.setTarget(null);
                return;
            }

            // Don't attack other custom golems
            if (attacker instanceof IronGolem && golemUUIDs.contains(attacker.getUniqueId())) {
                golem.setTarget(null);
                return;
            }

            // Make golem defend itself by targeting the attacker
            golem.setTarget(attacker);
            if (attacker instanceof Player) {
                golem.setAggressive(true);
            }
        }
    }

    // Handle when summoner attacks something - make golem help (checks 15 block range)
    public static void handleSummonerAttack(EntityDamageByEntityEvent event, Player summoner, TrustManager trustManager) {
        GolemInfo golemInfo = playerGolems.get(summoner.getUniqueId());
        if (golemInfo == null || golemInfo.golem == null || golemInfo.golem.isDead()) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            IronGolem golem = golemInfo.golem;

            // Check if golem is within 15 blocks of the summoner
            if (golem.getLocation().distance(summoner.getLocation()) > 15.0) {
                return; // Golem is too far away to help
            }

            // Don't attack other custom golems
            if (target instanceof IronGolem && golemUUIDs.contains(target.getUniqueId())) {
                return;
            }

            // Check trust system - don't attack trusted players
            if (target instanceof Player && trustManager.isTrusted(summoner, (Player) target)) {
                return;
            }

            // Double-check that the target is not the summoner themselves (edge case protection)
            if (target.getUniqueId().equals(summoner.getUniqueId())) {
                return;
            }

            // Make golem help attack the target
            golem.setTarget(target);
            if (target instanceof Player) {
                golem.setAggressive(true);
            }
        }
    }

    // Clean up method to be called when golems die naturally
    public static void cleanupGolem(IronGolem golem) {
        UUID golemUUID = golem.getUniqueId();
        golemUUIDs.remove(golemUUID);

        // Clear all potion effects before cleanup
        golem.clearActivePotionEffects();

        // Find and remove from playerGolems map
        playerGolems.entrySet().removeIf(entry -> {
            GolemInfo golemInfo = entry.getValue();
            return golemInfo.golem != null && golemInfo.golem.getUniqueId().equals(golemUUID);
        });
    }

    // Method to handle golem death events
    public static void handleGolemDeath(IronGolem golem) {
        if (golemUUIDs.contains(golem.getUniqueId())) {
            // Find the summoner to notify them
            Player summoner = null;
            for (GolemInfo golemInfo : playerGolems.values()) {
                if (golemInfo.golem != null && golemInfo.golem.equals(golem)) {
                    summoner = org.bukkit.Bukkit.getPlayer(golemInfo.summonerUUID);
                    break;
                }
            }

            if (summoner != null && summoner.isOnline()) {
                summoner.sendMessage(Component.text("💀 Your Golem Has Fallen!")
                        .color(NamedTextColor.RED));
            }

            cleanupGolem(golem);
        }
    }

    // Method to handle player logout - remove their golem
    public static void handlePlayerLogout(Player player) {
        GolemInfo golemInfo = playerGolems.get(player.getUniqueId());
        if (golemInfo != null && golemInfo.golem != null && !golemInfo.golem.isDead()) {
            // Remove golem when player logs out
            golemUUIDs.remove(golemInfo.golem.getUniqueId());
            golemInfo.golem.clearActivePotionEffects();
            golemInfo.golem.remove();
            playerGolems.remove(player.getUniqueId());
        }
    }

    // Method to get golem info for debugging/admin purposes
    public static String getGolemStats(IronGolem golem) {
        if (!golemUUIDs.contains(golem.getUniqueId())) {
            return null;
        }

        Player summoner = null;
        for (GolemInfo golemInfo : playerGolems.values()) {
            if (golemInfo.golem != null && golemInfo.golem.equals(golem)) {
                summoner = org.bukkit.Bukkit.getPlayer(golemInfo.summonerUUID);
                break;
            }
        }

        return String.format("Buddy - Health: %.1f/%.1f, Strength: %s, Summoner: %s",
                golem.getHealth(),
                golem.getMaxHealth(),
                golem.hasPotionEffect(PotionEffectType.STRENGTH) ? "Level 2" : "None",
                summoner != null ? summoner.getName() : "Unknown");
    }

    // Method to get distance between golem and summoner (for admin monitoring)
    public static double getGolemDistance(Player player) {
        GolemInfo golemInfo = playerGolems.get(player.getUniqueId());
        if (golemInfo == null || golemInfo.golem == null || golemInfo.golem.isDead()) {
            return -1; // No golem
        }

        if (!golemInfo.golem.getLocation().getWorld().equals(player.getLocation().getWorld())) {
            return -2; // Different worlds
        }

        return golemInfo.golem.getLocation().distance(player.getLocation());
    }

    // Method to check if player has an active golem
    public static boolean hasActiveGolem(Player player) {
        GolemInfo golemInfo = playerGolems.get(player.getUniqueId());
        return golemInfo != null && golemInfo.golem != null && !golemInfo.golem.isDead();
    }

    // Method to get all active golems count (for admin monitoring)
    public static int getActiveGolemCount() {
        return playerGolems.size();
    }

    // Method to force remove all golems (for admin cleanup)
    public static void removeAllGolems() {
        for (GolemInfo golemInfo : playerGolems.values()) {
            if (golemInfo.golem != null && !golemInfo.golem.isDead()) {
                golemInfo.golem.clearActivePotionEffects();
                golemInfo.golem.remove();
            }
        }
        playerGolems.clear();
        golemUUIDs.clear();
    }

    // Method to get maximum allowed distance
    public static double getMaxDistance() {
        return MAX_DISTANCE;
    }
}