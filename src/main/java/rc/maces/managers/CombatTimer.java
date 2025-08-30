package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
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

    // Enhanced safe zone system with file persistence
    private final Map<String, SafeZone> safeZones = new HashMap<>();
    private final File safeZoneFile;
    private final FileConfiguration safeZoneConfig;

    public CombatTimer(JavaPlugin plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;

        // Initialize safe zone file
        this.safeZoneFile = new File(plugin.getDataFolder(), "safezones.yml");
        this.safeZoneConfig = YamlConfiguration.loadConfiguration(safeZoneFile);

        loadSafeZonesFromFile();
        startCleanupTask();
    }

    /**
     * Enhanced SafeZone class with more features
     */
    public static class SafeZone {
        private final int centerX, centerZ, radius;
        private final String name;
        private final long createdTime;
        private final String createdBy;

        public SafeZone(int centerX, int centerZ, int radius, String name, String createdBy) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.name = name;
            this.createdTime = System.currentTimeMillis();
            this.createdBy = createdBy != null ? createdBy : "System";
        }

        // Legacy constructor for backwards compatibility
        public SafeZone(int centerX, int centerZ, int radius, String name) {
            this(centerX, centerZ, radius, name, "System");
        }

        public boolean isInside(Location location) {
            double distance = Math.sqrt(Math.pow(location.getX() - centerX, 2) + Math.pow(location.getZ() - centerZ, 2));
            return distance <= radius;
        }

        // Getters
        public int getCenterX() { return centerX; }
        public int getCenterZ() { return centerZ; }
        public int getRadius() { return radius; }
        public String getName() { return name; }
        public long getCreatedTime() { return createdTime; }
        public String getCreatedBy() { return createdBy; }

        public String getFormattedInfo() {
            return String.format("%s (%d, %d) radius %d - created by %s",
                    name, centerX, centerZ, radius, createdBy);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Check safe zones FIRST - highest priority protection
        if (isInSafeZone(victim.getLocation()) || isInSafeZone(attacker.getLocation())) {
            event.setCancelled(true);

            String victimZone = getSafeZoneName(victim.getLocation());
            String attackerZone = getSafeZoneName(attacker.getLocation());
            String zoneName = victimZone != null ? victimZone : attackerZone;

            attacker.sendMessage(Component.text("🏠 PvP is disabled in " + zoneName + "!")
                    .color(NamedTextColor.YELLOW));
            victim.sendMessage(Component.text("🛡 You are protected by " + zoneName + "!")
                    .color(NamedTextColor.GREEN));

            plugin.getLogger().info("PvP blocked in safe zone (" + zoneName + "): " +
                    attacker.getName() + " vs " + victim.getName());
            return;
        }

        // Check spawn protection
        if (hasSpawnProtection(victim) || hasSpawnProtection(attacker)) {
            event.setCancelled(true);

            if (hasSpawnProtection(victim)) {
                attacker.sendMessage(Component.text("🛡 " + victim.getName() + " has spawn protection!")
                        .color(NamedTextColor.YELLOW));
                victim.sendMessage(Component.text("🛡 Your spawn protection blocked the attack!")
                        .color(NamedTextColor.GREEN));
            }

            if (hasSpawnProtection(attacker)) {
                attacker.sendMessage(Component.text("🛡 You cannot attack while you have spawn protection!")
                        .color(NamedTextColor.YELLOW));
            }

            return;
        }

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

        // If we get here, it's valid PvP - put both players in combat
        putInCombat(victim, attacker);
        putInCombat(attacker, victim);

        plugin.getLogger().info("Combat initiated between " + attacker.getName() + " and " + victim.getName());
    }

    // ============ ENHANCED SAFE ZONE METHODS ============

    /**
     * Load safe zones from configuration file
     */
    private void loadSafeZonesFromFile() {
        try {
            if (safeZoneFile.exists()) {
                int loadedZones = 0;

                for (String worldName : safeZoneConfig.getKeys(false)) {
                    var zoneSection = safeZoneConfig.getConfigurationSection(worldName);
                    if (zoneSection != null) {
                        try {
                            int centerX = zoneSection.getInt("centerX", 0);
                            int centerZ = zoneSection.getInt("centerZ", 0);
                            int radius = zoneSection.getInt("radius", 100);
                            String name = zoneSection.getString("name", "Safe Zone");
                            String createdBy = zoneSection.getString("createdBy", "System");

                            SafeZone zone = new SafeZone(centerX, centerZ, radius, name, createdBy);
                            safeZones.put(worldName, zone);
                            loadedZones++;

                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load safe zone for world " + worldName + ": " + e.getMessage());
                        }
                    }
                }

                plugin.getLogger().info("Loaded " + loadedZones + " safe zones from configuration file");
            } else {
                // Create default spawn safe zone
                initializeDefaultSafeZones();
                saveSafeZonesToFile();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading safe zones: " + e.getMessage());
            initializeDefaultSafeZones();
        }
    }

    /**
     * Save safe zones to configuration file
     */
    private void saveSafeZonesToFile() {
        try {
            // Clear existing data
            for (String key : safeZoneConfig.getKeys(false)) {
                safeZoneConfig.set(key, null);
            }

            // Save current safe zones
            for (Map.Entry<String, SafeZone> entry : safeZones.entrySet()) {
                String worldName = entry.getKey();
                SafeZone zone = entry.getValue();

                safeZoneConfig.set(worldName + ".centerX", zone.getCenterX());
                safeZoneConfig.set(worldName + ".centerZ", zone.getCenterZ());
                safeZoneConfig.set(worldName + ".radius", zone.getRadius());
                safeZoneConfig.set(worldName + ".name", zone.getName());
                safeZoneConfig.set(worldName + ".createdBy", zone.getCreatedBy());
                safeZoneConfig.set(worldName + ".createdTime", zone.getCreatedTime());
            }

            safeZoneConfig.save(safeZoneFile);
            plugin.getLogger().info("Saved " + safeZones.size() + " safe zones to configuration file");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save safe zones: " + e.getMessage());
        }
    }

    /**
     * Initialize default safe zones
     */
    private void initializeDefaultSafeZones() {
        // Default spawn protection (modify coordinates as needed)
        safeZones.put("world", new SafeZone(0, 0, 100, "Spawn Area", "System"));
        plugin.getLogger().info("Created default spawn safe zone at (0, 0) with 100 block radius");
    }

    /**
     * Add a safe zone to a world with admin tracking
     */
    public void addSafeZone(String worldName, int centerX, int centerZ, int radius, String name, String createdBy) {
        SafeZone zone = new SafeZone(centerX, centerZ, radius, name, createdBy);
        safeZones.put(worldName, zone);
        saveSafeZonesToFile();

        plugin.getLogger().info("Added safe zone: " + name + " at " + worldName +
                " (" + centerX + ", " + centerZ + ") radius " + radius + " by " + createdBy);
    }

    /**
     * Add safe zone with default creator
     */
    public void addSafeZone(String worldName, int centerX, int centerZ, int radius, String name) {
        addSafeZone(worldName, centerX, centerZ, radius, name, "System");
    }

    /**
     * Remove a safe zone from a world
     */
    public void removeSafeZone(String worldName) {
        SafeZone removed = safeZones.remove(worldName);
        if (removed != null) {
            saveSafeZonesToFile();
            plugin.getLogger().info("Removed safe zone: " + removed.getName() + " from " + worldName);
        }
    }

    /**
     * Check if a location is in a safe zone
     */
    public boolean isInSafeZone(Location location) {
        if (location == null || location.getWorld() == null) return false;

        String worldName = location.getWorld().getName();
        SafeZone safeZone = safeZones.get(worldName);

        return safeZone != null && safeZone.isInside(location);
    }

    /**
     * Get the name of the safe zone at a location (null if not in safe zone)
     */
    public String getSafeZoneName(Location location) {
        if (location == null || location.getWorld() == null) return null;

        String worldName = location.getWorld().getName();
        SafeZone safeZone = safeZones.get(worldName);

        if (safeZone != null && safeZone.isInside(location)) {
            return safeZone.getName();
        }

        return null;
    }

    /**
     * Get safe zone details for a location
     */
    public SafeZone getSafeZone(Location location) {
        if (location == null || location.getWorld() == null) return null;

        String worldName = location.getWorld().getName();
        SafeZone safeZone = safeZones.get(worldName);

        if (safeZone != null && safeZone.isInside(location)) {
            return safeZone;
        }

        return null;
    }

    /**
     * Get safe zone for a specific world
     */
    public SafeZone getSafeZoneForWorld(String worldName) {
        return safeZones.get(worldName);
    }

    /**
     * Get all safe zones (for admin commands)
     */
    public Map<String, SafeZone> getSafeZones() {
        return new HashMap<>(safeZones);
    }

    /**
     * Get distance from nearest safe zone
     */
    public double getDistanceToNearestSafeZone(Location location) {
        if (location == null || location.getWorld() == null) return Double.MAX_VALUE;

        String worldName = location.getWorld().getName();
        SafeZone safeZone = safeZones.get(worldName);

        if (safeZone == null) return Double.MAX_VALUE;

        double distance = Math.sqrt(Math.pow(location.getX() - safeZone.getCenterX(), 2) +
                Math.pow(location.getZ() - safeZone.getCenterZ(), 2));

        // Return distance to edge of safe zone (negative if inside)
        return distance - safeZone.getRadius();
    }

    /**
     * Check if a player can enter combat at their current location
     */
    public boolean canEnterCombatAt(Location location) {
        return !isInSafeZone(location);
    }

    // ============ COMBAT TIMER METHODS ============

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Give new players spawn protection
        giveSpawnProtection(player, "joined the server");
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
        spawnProtection.remove(player.getUniqueId());

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

        // Check if player is in a safe zone - don't put them in combat if they are
        if (isInSafeZone(player.getLocation())) {
            plugin.getLogger().info("Prevented combat timer for " + player.getName() + " (in safe zone)");
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

        // Show safe zone status with enhanced info
        SafeZone currentZone = getSafeZone(player.getLocation());
        if (currentZone != null) {
            player.sendMessage(Component.text("🏠 LOCATION: " + currentZone.getName() + " (Safe Zone)")
                    .color(NamedTextColor.GREEN));

            double distanceFromCenter = Math.sqrt(
                    Math.pow(player.getLocation().getX() - currentZone.getCenterX(), 2) +
                            Math.pow(player.getLocation().getZ() - currentZone.getCenterZ(), 2)
            );
            int edgeDistance = currentZone.getRadius() - (int)distanceFromCenter;

            player.sendMessage(Component.text("📏 Distance to edge: " + edgeDistance + " blocks")
                    .color(NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("🏠 LOCATION: PvP Zone")
                    .color(NamedTextColor.RED));

            double distanceToSafeZone = getDistanceToNearestSafeZone(player.getLocation());
            if (distanceToSafeZone != Double.MAX_VALUE && distanceToSafeZone > 0) {
                player.sendMessage(Component.text("🛡 Nearest safe zone: " + (int)distanceToSafeZone + " blocks away")
                        .color(NamedTextColor.GRAY));
            }
        }

        player.sendMessage(Component.text("═══════════════════════════")
                .color(NamedTextColor.GOLD));
    }

    // ============ SPAWN PROTECTION METHODS ============

    /**
     * Give a player spawn protection
     */
    public void giveSpawnProtection(Player player, String reason) {
        if (player == null || !player.isOnline()) return;

        spawnProtection.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage(Component.text("🛡 Spawn protection granted for " + (SPAWN_PROTECTION_TIME/1000) + " seconds (" + reason + ")")
                .color(NamedTextColor.AQUA));

        plugin.getLogger().info("Granted spawn protection to " + player.getName() + ": " + reason);
    }

    /**
     * Check if a player has spawn protection
     */
    public boolean hasSpawnProtection(Player player) {
        if (player == null) return false;

        Long protectionTime = spawnProtection.get(player.getUniqueId());
        if (protectionTime == null) return false;

        if (System.currentTimeMillis() - protectionTime > SPAWN_PROTECTION_TIME) {
            spawnProtection.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    /**
     * Get remaining spawn protection time in milliseconds
     */
    public long getRemainingSpawnProtection(Player player) {
        if (player == null) return 0;

        Long protectionTime = spawnProtection.get(player.getUniqueId());
        if (protectionTime == null) return 0;

        long remaining = SPAWN_PROTECTION_TIME - (System.currentTimeMillis() - protectionTime);
        if (remaining <= 0) {
            spawnProtection.remove(player.getUniqueId());
            return 0;
        }

        return remaining;
    }

    /**
     * Remove spawn protection from a player
     */
    public void removeSpawnProtection(Player player, String reason) {
        if (player == null) return;

        if (spawnProtection.remove(player.getUniqueId()) != null) {
            player.sendMessage(Component.text("🛡 Spawn protection removed: " + reason)
                    .color(NamedTextColor.GRAY));
            plugin.getLogger().info("Removed spawn protection from " + player.getName() + ": " + reason);
        }
    }

    // ============ CLEANUP AND UTILITY METHODS ============

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
     * Check two specific players would trigger combat with each other
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
     * Get the spawn protection duration in milliseconds
     */
    public static long getSpawnProtectionDuration() {
        return SPAWN_PROTECTION_TIME;
    }

    /**
     * Check if the combat timer system is working properly
     */
    public boolean isSystemHealthy() {
        // Basic health check - could be expanded
        return plugin != null && trustManager != null;
    }

    /**
     * Create a safe zone at current location with creator tracking
     */
    public void addSafeZone(String worldName, int centerX, int centerZ, int radius, String name, Player creator) {
        String createdBy = creator != null ? creator.getName() : "System";
        addSafeZone(worldName, centerX, centerZ, radius, name, createdBy);
    }

    /**
     * Save all safe zone data
     */
    public void saveAllData() {
        saveSafeZonesToFile();
    }
}