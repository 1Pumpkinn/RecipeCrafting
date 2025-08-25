package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.*;

// UPDATED BuddyUp Ability - Summons a protective iron golem that properly defends the summoner in 8 block range
public class BuddyUpAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;
    private static final Map<UUID, GolemInfo> playerGolems = new HashMap<>();
    private static final Set<UUID> golemUUIDs = new HashSet<>();

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
        super("buddy_up", 15, cooldownManager);
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

        // Set health to 10 hearts (20 HP)
        golem.setMaxHealth(20.0);
        golem.setHealth(20.0);

        // Make it protect the player
        golem.setCustomName("§a" + player.getName() + "'s Buddy");
        golem.setCustomNameVisible(true);

        // Make the golem not target its summoner initially
        golem.setTarget(null);

        // Store the golem with summoner info
        GolemInfo golemInfo = new GolemInfo(golem, player.getUniqueId());
        playerGolems.put(player.getUniqueId(), golemInfo);
        golemUUIDs.add(golem.getUniqueId());

        player.sendMessage(Component.text("🤖 BUDDY UP! Your iron golem protector has arrived!")
                .color(NamedTextColor.GREEN));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.8f);

        // Remove golem after 60 seconds if still alive
        new BukkitRunnable() {
            @Override
            public void run() {
                removeExistingGolem(player);
            }
        }.runTaskLater(plugin, 1200L);

        setCooldown(player);
    }

    private void removeExistingGolem(Player player) {
        GolemInfo existingGolemInfo = playerGolems.remove(player.getUniqueId());
        if (existingGolemInfo != null && existingGolemInfo.golem != null && !existingGolemInfo.golem.isDead()) {
            golemUUIDs.remove(existingGolemInfo.golem.getUniqueId());
            existingGolemInfo.golem.remove();
        }
    }

    // UPDATED: Handle when the player gets damaged by ANY living entity (now checks 8 block range)
    public static void handlePlayerDamage(EntityDamageByEntityEvent event, Player victim, TrustManager trustManager) {
        GolemInfo golemInfo = playerGolems.get(victim.getUniqueId());
        if (golemInfo == null || golemInfo.golem == null || golemInfo.golem.isDead()) {
            return;
        }

        IronGolem golem = golemInfo.golem;

        // UPDATED: Check if golem is within 8 blocks of the victim
        if (golem.getLocation().distance(victim.getLocation()) > 8.0) {
            return; // Golem is too far away to help
        }

        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();

            // Don't make golem attack its own summoner (this was the bug)
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

    // Handle when a golem gets damaged - prevent it from attacking its own summoner
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

        // If the summoner is attacking their own golem, prevent the golem from retaliating
        if (event.getDamager().getUniqueId().equals(summoner.getUniqueId())) {
            // Clear the golem's target if it's targeting its summoner
            if (golem.getTarget() != null && golem.getTarget().getUniqueId().equals(summoner.getUniqueId())) {
                golem.setTarget(null);
                golem.setAggressive(false);
            }
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

    // UPDATED: Handle when summoner attacks something - make golem help (now checks 8 block range)
    public static void handleSummonerAttack(EntityDamageByEntityEvent event, Player summoner, TrustManager trustManager) {
        GolemInfo golemInfo = playerGolems.get(summoner.getUniqueId());
        if (golemInfo == null || golemInfo.golem == null || golemInfo.golem.isDead()) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            IronGolem golem = golemInfo.golem;

            // UPDATED: Check if golem is within 8 blocks of the summoner
            if (golem.getLocation().distance(summoner.getLocation()) > 8.0) {
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

        // Find and remove from playerGolems map
        playerGolems.entrySet().removeIf(entry -> {
            GolemInfo golemInfo = entry.getValue();
            return golemInfo.golem != null && golemInfo.golem.getUniqueId().equals(golemUUID);
        });
    }

    // Method to handle golem death events
    public static void handleGolemDeath(IronGolem golem) {
        if (golemUUIDs.contains(golem.getUniqueId())) {
            cleanupGolem(golem);
        }
    }
}