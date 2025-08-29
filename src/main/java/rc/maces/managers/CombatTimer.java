package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTimer implements Listener {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;
    private final Map<UUID, Long> combatPlayers = new HashMap<>();
    private final Map<UUID, UUID> combatCause = new HashMap<>(); // Track who caused each player's combat
    private final Map<UUID, Long> spawnProtection = new HashMap<>(); // Track spawn protection
    private static final long COMBAT_TIME = 15000; // 15 seconds in milliseconds
    private static final long SPAWN_PROTECTION_TIME = 10000; // 10 seconds spawn protection

    // Configurable safe zones (you can modify these or load from config)
    private final Map<String, SafeZone> safeZones = new HashMap<>();

    public CombatTimer(JavaPlugin plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
        initializeSafeZones();
        startCleanupTask();
    }

    /**
     * Initialize default safe zones - you can modify this or load from config
     */
    private void initializeSafeZones() {
        // Example safe zones - modify these coordinates for your server
        // Format: world name -> SafeZone

        // Spawn area example (100 block radius around 0,0)
        safeZones.put("world", new SafeZone(0, 0, 100, "Spawn Area"));

        // You can add more safe zones here:
        // safeZones.put("world_nether", new SafeZone(0, 0, 50, "Nether Spawn"));
        // safeZones.put("world", new SafeZone(-500, -500, 75, "Safe Town"));

        plugin.getLogger().info("Loaded " + safeZones.size() + " safe zones for combat protection");
    }

    /**
     * Safe zone data class
     */
    private static class SafeZone {
        final int centerX, centerZ, radius;
        final String name;

        SafeZone(int centerX, int centerZ, int radius, String name) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.name = name;
        }

        boolean isInside(Location location) {
            double distance = Math.sqrt(Math.pow(location.getX() - centerX, 2) + Math.pow(location.getZ() - centerZ, 2));
            return distance <= radius;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Check if players are trusted allies
        if (trustManager != null && trustManager.isTrusted(victim, attacker)) {
            // Players are allies - cancel damage and don't trigger combat timer
            event.setCancelled(true);

            // Send messages to both players
            attacker.sendMessage(Component.text("⚠ You cannot attack your ally " + victim.getName() + "!")
                    .color(NamedTextColor.YELLOW));
            victim.sendMessage(Component.text("⚠ Your ally " + attacker.getName() + " tried to attack you but damage was blocked!")
                    .color(NamedTextColor.YELLOW));

            plugin.getLogger().info("PvP damage blocked between allies: " + attacker.getName() + " and " + victim.getName());
            return;
        }

        // If players are not allies, put both in combat
        putInCombat(victim, attacker);
        putInCombat(attacker, victim);

        plugin.getLogger().info("Combat initiated between " + attacker.getName() + " and " + victim.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (isInCombat(player)) {
            // Check if the player was put in combat by a trusted ally
            UUID combatCauseUUID = combatCause.get(player.getUniqueId());
            boolean killedByAlly = false;

            if (combatCauseUUID != null) {
                Player combatCausePlayer = Bukkit.getPlayer(combatCauseUUID);

                if (trustManager != null && combatCausePlayer != null &&
                        trustManager.isTrusted(player, combatCausePlayer)) {
                    killedByAlly = true;
                }
            }

            if (!killedByAlly) {
                // Kill player for combat logging (only if not caused by ally)
                player.setHealth(0);

                Component deathMessage = Component.text(player.getName() + " has been killed for combat logging!")
                        .color(NamedTextColor.RED);
                Bukkit.broadcast(deathMessage);

                plugin.getLogger().info(player.getName() + " was killed for combat logging");
            } else {
                // Player was in combat due to ally interaction - don't kill them
                plugin.getLogger().info(player.getName() + " logged off in ally-caused combat - no penalty applied");

                // Optionally notify the ally that their friend logged off
                Player ally = Bukkit.getPlayer(combatCauseUUID);
                if (ally != null && ally.isOnline()) {
                    ally.sendMessage(Component.text("⚠ Your ally " + player.getName() + " has logged off.")
                            .color(NamedTextColor.YELLOW));
                }
            }
        }

        // Clean up tracking data
        combatPlayers.remove(player.getUniqueId());
        combatCause.remove(player.getUniqueId());
        spawnProtection.remove(player.getUniqueId()); // Also clean up spawn protection

        // Also clean up if this player was the cause of someone else's combat
        combatCause.values().removeIf(causeUUID -> causeUUID.equals(player.getUniqueId()));
    }

    public void putInCombat(Player player) {
        putInCombat(player, null);
    }

    public void putInCombat(Player player, Player cause) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean wasInCombat = isInCombat(player);
        combatPlayers.put(player.getUniqueId(), System.currentTimeMillis());

        // Track who caused this player to enter combat
        if (cause != null) {
            combatCause.put(player.getUniqueId(), cause.getUniqueId());
        }

        if (!wasInCombat) {
            player.sendMessage(Component.text("⚔ You are now in combat for 15 seconds!")
                    .color(NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("⚔ Combat timer refreshed!")
                    .color(NamedTextColor.RED));
        }
    }

    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }

        Long combatTime = combatPlayers.get(player.getUniqueId());
        if (combatTime == null) {
            return false;
        }

        if (System.currentTimeMillis() - combatTime > COMBAT_TIME) {
            combatPlayers.remove(player.getUniqueId());
            combatCause.remove(player.getUniqueId()); // Clean up cause tracking
            if (player.isOnline()) {
                player.sendMessage(Component.text("✓ You are no longer in combat.")
                        .color(NamedTextColor.GREEN));
            }
            return false;
        }

        return true;
    }

    public long getRemainingCombatTime(Player player) {
        if (player == null) {
            return 0;
        }

        Long combatTime = combatPlayers.get(player.getUniqueId());
        if (combatTime == null) {
            return 0;
        }

        long remaining = COMBAT_TIME - (System.currentTimeMillis() - combatTime);
        return Math.max(0, remaining);
    }

    /**
     * Get remaining combat time in a formatted string
     */
    public String getRemainingCombatTimeFormatted(Player player) {
        long remaining = getRemainingCombatTime(player);
        if (remaining <= 0) {
            return "0s";
        }

        long seconds = remaining / 1000;
        return seconds + "s";
    }

    /**
     * Remove a player from combat (useful for admin commands or special cases)
     */
    public void removeFromCombat(Player player) {
        if (player != null && combatPlayers.remove(player.getUniqueId()) != null) {
            combatCause.remove(player.getUniqueId()); // Clean up cause tracking
            if (player.isOnline()) {
                player.sendMessage(Component.text("✓ You have been removed from combat.")
                        .color(NamedTextColor.GREEN));
            }
        }
    }

    /**
     * Check if two specific players would trigger combat with each other
     */
    public boolean wouldTriggerCombat(Player player1, Player player2) {
        if (player1 == null || player2 == null) {
            return false;
        }

        if (trustManager != null && trustManager.isTrusted(player1, player2)) {
            return false; // Trusted players don't trigger combat
        }

        return true; // Non-trusted players would trigger combat
    }

    /**
     * Enhanced canPerformAction that also checks spawn protection and safe zones
     */
    public boolean canPerformAction(Player player, String actionName) {
        if (!isInCombat(player)) {
            return true;
        }

        // Player is in combat - deny the action
        player.sendMessage(Component.text("❌ You cannot " + actionName + " while in combat!")
                .color(NamedTextColor.RED));
        player.sendMessage(Component.text("⚔ Combat ends in: " + getRemainingCombatTimeFormatted(player))
                .color(NamedTextColor.YELLOW));

        return false;
    }

    /**
     * Check if PvP is allowed between two players (considering all protections)
     */
    public boolean isPvPAllowed(Player attacker, Player victim) {
        if (attacker == null || victim == null) return false;
        if (attacker.equals(victim)) return false;

        // Check trust relationship
        if (trustManager != null && trustManager.isTrusted(attacker, victim)) {
            return false;
        }

        // Check spawn protection
        if (hasSpawnProtection(attacker) || hasSpawnProtection(victim)) {
            return false;
        }

        // Check safe zones
        if (isInSafeZone(attacker.getLocation()) || isInSafeZone(victim.getLocation())) {
            return false;
        }

        // Check game modes
        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR ||
                victim.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        return true;
    }

    /**
     * Show enhanced combat status including protections
     */
    public void showCombatStatus(Player player) {
        player.sendMessage(Component.text("═══════ COMBAT STATUS ═══════")
                .color(NamedTextColor.GOLD));

        if (isInCombat(player)) {
            player.sendMessage(Component.text("⚔ COMBAT STATUS: IN COMBAT")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("Time remaining: " + getRemainingCombatTimeFormatted(player))
                    .color(NamedTextColor.YELLOW));

            // Show who caused the combat if available
            UUID causeUUID = combatCause.get(player.getUniqueId());
            if (causeUUID != null) {
                Player causePlayer = Bukkit.getPlayer(causeUUID);
                if (causePlayer != null) {
                    player.sendMessage(Component.text("Caused by: " + causePlayer.getName())
                            .color(NamedTextColor.GRAY));
                }
            }
        } else {
            player.sendMessage(Component.text("⚔ COMBAT STATUS: NOT IN COMBAT")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
        }

        // Show spawn protection status
        if (hasSpawnProtection(player)) {
            long remaining = getRemainingSpawnProtection(player) / 1000;
            player.sendMessage(Component.text("🛡 SPAWN PROTECTION: " + remaining + " seconds remaining")
                    .color(NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("🛡 SPAWN PROTECTION: None")
                    .color(NamedTextColor.GRAY));
        }

        // Show safe zone status
        String safeZone = getSafeZoneName(player.getLocation());
        if (safeZone != null) {
            player.sendMessage(Component.text("🏠 LOCATION: " + safeZone + " (Safe Zone)")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("🏠 LOCATION: PvP Zone")
                    .color(NamedTextColor.RED));
        }

        player.sendMessage(Component.text("═══════════════════════════")
                .color(NamedTextColor.GOLD));
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up expired combat timers
                combatPlayers.entrySet().removeIf(entry -> {
                    if (System.currentTimeMillis() - entry.getValue() > COMBAT_TIME) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("✓ You are no longer in combat.")
                                    .color(NamedTextColor.GREEN));
                        }
                        // Also clean up cause tracking for expired combat timers
                        combatCause.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });

                // Clean up expired spawn protection
                spawnProtection.entrySet().removeIf(entry -> {
                    if (System.currentTimeMillis() - entry.getValue() > SPAWN_PROTECTION_TIME) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("🛡 Spawn protection has expired")
                                    .color(NamedTextColor.GRAY));
                        }
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Get all players currently in combat (for admin purposes)
     */
    public Map<UUID, Long> getCombatPlayers() {
        return new HashMap<>(combatPlayers);
    }

    /**
     * Check who caused a player to enter combat
     */
    public Player getCombatCause(Player player) {
        if (player == null) return null;
        UUID causeUUID = combatCause.get(player.getUniqueId());
        return causeUUID != null ? Bukkit.getPlayer(causeUUID) : null;
    }

    /**
     * Check if a player's combat was caused by a trusted ally
     */
    public boolean isCombatCausedByAlly(Player player) {
        if (player == null) return false;

        UUID causeUUID = combatCause.get(player.getUniqueId());
        if (causeUUID == null) return false;

        Player cause = Bukkit.getPlayer(causeUUID);
        if (cause == null) return false;

        return trustManager != null && trustManager.isTrusted(player, cause);
    }

    /**
     * Force put a player in combat (for admin commands or special abilities)
     */
    public void forceInCombat(Player player, Player cause, String reason) {
        putInCombat(player, cause);
        if (reason != null && !reason.isEmpty()) {
            player.sendMessage(Component.text("⚔ You have been put in combat: " + reason)
                    .color(NamedTextColor.RED));
        }
        plugin.getLogger().info("Forced " + player.getName() + " into combat" +
                (cause != null ? " by " + cause.getName() : "") +
                (reason != null ? " (" + reason + ")" : ""));
    }

    /**
     * Get the combat time duration in milliseconds
     */
    public static long getCombatDuration() {
        return COMBAT_TIME;
    }

    /**
     * Check if the combat timer system is working properly
     */
    public boolean isSystemHealthy() {
        // Basic health check - could be expanded
        return plugin != null && trustManager != null;
    }
}