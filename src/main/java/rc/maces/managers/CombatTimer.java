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
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
    private static final long COMBAT_TIME = 15000; // 15 seconds in milliseconds

    // Enhanced safe zone system with cuboid support
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
     * Enhanced SafeZone class that supports cuboid regions (pos1 to pos2)
     */
    public static class SafeZone {
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private final String name;
        private final long createdTime;
        private final String createdBy;

        public SafeZone(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String name, String createdBy) {
            // Ensure min/max are correct
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);
            this.name = name;
            this.createdTime = System.currentTimeMillis();
            this.createdBy = createdBy != null ? createdBy : "System";
        }

        // Legacy constructor for backwards compatibility (circular to cuboid conversion)
        public SafeZone(int centerX, int centerZ, int radius, String name, String createdBy) {
            this(centerX - radius, -64, centerZ - radius,
                    centerX + radius, 320, centerZ + radius, name, createdBy);
        }

        /**
         * Check if a location is inside this cuboid safe zone
         */
        public boolean isInside(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }

            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            return x >= minX && x <= maxX &&
                    y >= minY && y <= maxY &&
                    z >= minZ && z <= maxZ;
        }

        /**
         * Get the closest distance to the edge of the safe zone
         */
        public double getDistanceToEdge(Location location) {
            if (location == null) return Double.MAX_VALUE;
            if (isInside(location)) return -1; // Inside the zone

            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            // Calculate distance to the closest face of the cuboid
            double dx = Math.max(0, Math.max(minX - x, x - maxX));
            double dy = Math.max(0, Math.max(minY - y, y - maxY));
            double dz = Math.max(0, Math.max(minZ - z, z - maxZ));

            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /**
         * Get center coordinates of the safe zone
         */
        public int getCenterX() { return (minX + maxX) / 2; }
        public int getCenterY() { return (minY + maxY) / 2; }
        public int getCenterZ() { return (minZ + maxZ) / 2; }

        // Getters for bounds
        public int getMinX() { return minX; }
        public int getMinY() { return minY; }
        public int getMinZ() { return minZ; }
        public int getMaxX() { return maxX; }
        public int getMaxY() { return maxY; }
        public int getMaxZ() { return maxZ; }

        // Size getters
        public int getSizeX() { return maxX - minX + 1; }
        public int getSizeY() { return maxY - minY + 1; }
        public int getSizeZ() { return maxZ - minZ + 1; }
        public long getTotalBlocks() { return (long)getSizeX() * getSizeY() * getSizeZ(); }

        // Info getters
        public String getName() { return name; }
        public long getCreatedTime() { return createdTime; }
        public String getCreatedBy() { return createdBy; }

        public String getFormattedInfo() {
            return String.format("%s [%d,%d,%d to %d,%d,%d] - created by %s",
                    name, minX, minY, minZ, maxX, maxY, maxZ, createdBy);
        }

        /**
         * Get coordinates as a formatted string
         */
        public String getCoordinatesString() {
            return String.format("From: %d, %d, %d  To: %d, %d, %d",
                    minX, minY, minZ, maxX, maxY, maxZ);
        }

        /**
         * Check if this safe zone overlaps with another cuboid
         */
        public boolean overlaps(int otherMinX, int otherMinY, int otherMinZ,
                                int otherMaxX, int otherMaxY, int otherMaxZ) {
            return !(maxX < otherMinX || minX > otherMaxX ||
                    maxY < otherMinY || minY > otherMaxY ||
                    maxZ < otherMinZ || minZ > otherMaxZ);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        // Handle direct player damage
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Handle projectile damage
        else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();

            // Check if the projectile was shot by a player
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();

                plugin.getLogger().info("Projectile hit: " + attacker.getName() + " hit " + victim.getName() +
                        " with " + projectile.getType());
            }
        }

        // If no player attacker found, let the damage through without combat consequences
        if (attacker == null) {
            return;
        }

        // Don't process self-damage
        if (attacker.equals(victim)) {
            return;
        }

        // Check safe zones FIRST - highest priority protection
        boolean attackerInSafeZone = isInSafeZone(attacker.getLocation());
        boolean victimInSafeZone = isInSafeZone(victim.getLocation());

        if (attackerInSafeZone || victimInSafeZone) {
            // Cancel the damage
            event.setCancelled(true);

            String victimZone = getSafeZoneName(victim.getLocation());
            String attackerZone = getSafeZoneName(attacker.getLocation());
            String zoneName = victimZone != null ? victimZone : attackerZone;

            // Send appropriate messages to both players
            if (attackerInSafeZone) {
                attacker.sendMessage(Component.text("🛡 You cannot attack from within a safe zone!")
                        .color(NamedTextColor.YELLOW));
            }
            if (victimInSafeZone) {
                attacker.sendMessage(Component.text("🛡 You cannot attack players in safe zones!")
                        .color(NamedTextColor.YELLOW));
            }

            plugin.getLogger().info("PvP blocked in safe zone (" + zoneName + "): " +
                    attacker.getName() + " vs " + victim.getName() +
                    (event.getDamager() instanceof Projectile ? " (projectile)" : " (direct)"));
            return;
        }

        // Check if players are trusted allies
        if (trustManager != null && trustManager.isTrusted(victim, attacker)) {
            // Players are allies - cancel damage and don't trigger combat timer
            event.setCancelled(true);

            // Send messages to both players
            String weaponType = event.getDamager() instanceof Projectile ? "projectile" : "attack";
            attacker.sendMessage(Component.text("⚠ Your " + weaponType + " cannot harm your ally " + victim.getName() + "!")
                    .color(NamedTextColor.YELLOW));
            victim.sendMessage(Component.text("⚠ Your ally " + attacker.getName() + " tried to hit you with " + weaponType + " but damage was blocked!")
                    .color(NamedTextColor.YELLOW));

            plugin.getLogger().info("PvP damage blocked between allies: " + attacker.getName() + " and " + victim.getName() +
                    (event.getDamager() instanceof Projectile ? " (projectile)" : " (direct)"));
            return;
        }

        // If we get here, it's valid PvP - put both players in combat
        putInCombat(victim, attacker);
        putInCombat(attacker, victim);

        String damageType = event.getDamager() instanceof Projectile ? " (projectile)" : " (direct)";
        plugin.getLogger().info("Combat initiated between " + attacker.getName() + " and " + victim.getName() + damageType);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        // Check if the dead player was in combat
        if (isInCombat(deadPlayer)) {
            // Find who caused the dead player's combat (the killer)
            UUID killerUUID = combatCause.get(deadPlayer.getUniqueId());
            Player killer = killerUUID != null ? Bukkit.getPlayer(killerUUID) : null;

            // Remove the dead player from combat immediately
            combatPlayers.remove(deadPlayer.getUniqueId());
            combatCause.remove(deadPlayer.getUniqueId());

            // If we found the killer and they're online and in combat, remove them too
            if (killer != null && killer.isOnline() && isInCombat(killer)) {
                // Check if the killer's combat was caused by the now-dead player
                UUID killersCombatCause = combatCause.get(killer.getUniqueId());
                if (killersCombatCause != null && killersCombatCause.equals(deadPlayer.getUniqueId())) {
                    // The killer was only in combat with the dead player, remove them from combat
                    combatPlayers.remove(killer.getUniqueId());
                    combatCause.remove(killer.getUniqueId());

                    killer.sendMessage(Component.text("✓ You are no longer in combat (opponent died).")
                            .color(NamedTextColor.GREEN));

                    plugin.getLogger().info("Removed " + killer.getName() + " from combat (killed " + deadPlayer.getName() + ")");
                }
            }

            // Clean up any combat relationships where the dead player was the cause
            // This handles cases where the dead player was fighting multiple people
            combatCause.entrySet().removeIf(entry -> {
                if (entry.getValue().equals(deadPlayer.getUniqueId())) {
                    // Someone was in combat because of the dead player
                    UUID otherPlayerUUID = entry.getKey();
                    Player otherPlayer = Bukkit.getPlayer(otherPlayerUUID);

                    if (otherPlayer != null && otherPlayer.isOnline()) {
                        // Remove the other player from combat since their opponent died
                        combatPlayers.remove(otherPlayerUUID);
                        otherPlayer.sendMessage(Component.text("✓ You are no longer in combat (opponent died).")
                                .color(NamedTextColor.GREEN));

                        plugin.getLogger().info("Removed " + otherPlayer.getName() + " from combat (opponent " + deadPlayer.getName() + " died)");
                    }
                    return true; // Remove this entry
                }
                return false;
            });

            plugin.getLogger().info("Player death ended combat for " + deadPlayer.getName() +
                    (killer != null ? " (killed by " + killer.getName() + ")" : ""));
        }
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

        // Also clean up if this player was the cause of someone else's combat
        combatCause.values().removeIf(causeUUID -> causeUUID.equals(player.getUniqueId()));
    }

    // ============ ENHANCED SAFE ZONE METHODS ============

    /**
     * Load safe zones from configuration file (now supports cuboid format)
     */
    private void loadSafeZonesFromFile() {
        try {
            if (safeZoneFile.exists()) {
                int loadedZones = 0;

                for (String worldName : safeZoneConfig.getKeys(false)) {
                    var zoneSection = safeZoneConfig.getConfigurationSection(worldName);
                    if (zoneSection != null) {
                        try {
                            // Try new cuboid format first
                            if (zoneSection.contains("minX")) {
                                int minX = zoneSection.getInt("minX");
                                int minY = zoneSection.getInt("minY");
                                int minZ = zoneSection.getInt("minZ");
                                int maxX = zoneSection.getInt("maxX");
                                int maxY = zoneSection.getInt("maxY");
                                int maxZ = zoneSection.getInt("maxZ");
                                String name = zoneSection.getString("name", "Safe Zone");
                                String createdBy = zoneSection.getString("createdBy", "System");

                                SafeZone zone = new SafeZone(minX, minY, minZ, maxX, maxY, maxZ, name, createdBy);
                                safeZones.put(worldName, zone);
                                loadedZones++;

                                plugin.getLogger().info("Loaded cuboid safe zone: " + name + " in " + worldName);
                            }
                            // Fallback to old circular format and convert
                            else if (zoneSection.contains("centerX")) {
                                int centerX = zoneSection.getInt("centerX", 0);
                                int centerZ = zoneSection.getInt("centerZ", 0);
                                int radius = zoneSection.getInt("radius", 100);
                                String name = zoneSection.getString("name", "Safe Zone");
                                String createdBy = zoneSection.getString("createdBy", "System");

                                // Convert circular to cuboid (full height)
                                SafeZone zone = new SafeZone(centerX, centerZ, radius, name, createdBy);
                                safeZones.put(worldName, zone);
                                loadedZones++;

                                plugin.getLogger().info("Converted circular safe zone to cuboid: " + name + " in " + worldName);
                            }

                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load safe zone for world " + worldName + ": " + e.getMessage());
                        }
                    }
                }

                plugin.getLogger().info("Loaded " + loadedZones + " safe zones from configuration file");

                // Save back to file to convert any old circular zones to new format
                if (loadedZones > 0) {
                    saveSafeZonesToFile();
                }
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
     * Save safe zones to configuration file (cuboid format)
     */
    private void saveSafeZonesToFile() {
        try {
            // Clear existing data
            for (String key : safeZoneConfig.getKeys(false)) {
                safeZoneConfig.set(key, null);
            }

            // Save current safe zones in new cuboid format
            for (Map.Entry<String, SafeZone> entry : safeZones.entrySet()) {
                String worldName = entry.getKey();
                SafeZone zone = entry.getValue();

                safeZoneConfig.set(worldName + ".minX", zone.getMinX());
                safeZoneConfig.set(worldName + ".minY", zone.getMinY());
                safeZoneConfig.set(worldName + ".minZ", zone.getMinZ());
                safeZoneConfig.set(worldName + ".maxX", zone.getMaxX());
                safeZoneConfig.set(worldName + ".maxY", zone.getMaxY());
                safeZoneConfig.set(worldName + ".maxZ", zone.getMaxZ());
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
        // Default spawn protection cuboid (100x100 area from Y -64 to 320)
        safeZones.put("world", new SafeZone(-50, -64, -50, 50, 320, 50, "Spawn Area", "System"));
        plugin.getLogger().info("Created default spawn safe zone: 100x384x100 cuboid at spawn");
    }

    /**
     * Add a cuboid safe zone using coordinates
     */
    public void addSafeZone(String worldName, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, String name, String createdBy) {
        SafeZone zone = new SafeZone(minX, minY, minZ, maxX, maxY, maxZ, name, createdBy);
        safeZones.put(worldName, zone);
        saveSafeZonesToFile();

        plugin.getLogger().info("Added cuboid safe zone: " + name + " in " + worldName +
                " [" + zone.getMinX() + "," + zone.getMinY() + "," + zone.getMinZ() +
                " to " + zone.getMaxX() + "," + zone.getMaxY() + "," + zone.getMaxZ() + "] by " + createdBy);
    }

    /**
     * Add safe zone with Player creator
     */
    public void addSafeZone(String worldName, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, String name, Player creator) {
        String createdBy = creator != null ? creator.getName() : "System";
        addSafeZone(worldName, minX, minY, minZ, maxX, maxY, maxZ, name, createdBy);
    }

    /**
     * Legacy method for backwards compatibility (converts circular to cuboid)
     */
    public void addSafeZone(String worldName, int centerX, int centerZ, int radius, String name, String createdBy) {
        addSafeZone(worldName, centerX - radius, -64, centerZ - radius,
                centerX + radius, 320, centerZ + radius, name, createdBy);
    }

    /**
     * Legacy method with Player creator
     */
    public void addSafeZone(String worldName, int centerX, int centerZ, int radius, String name, Player creator) {
        String createdBy = creator != null ? creator.getName() : "System";
        addSafeZone(worldName, centerX, centerZ, radius, name, createdBy);
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
     * Check if a location is in a safe zone (now cuboid-based)
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
     * Get distance from nearest safe zone (now uses cuboid distance)
     */
    public double getDistanceToNearestSafeZone(Location location) {
        if (location == null || location.getWorld() == null) return Double.MAX_VALUE;

        String worldName = location.getWorld().getName();
        SafeZone safeZone = safeZones.get(worldName);

        if (safeZone == null) return Double.MAX_VALUE;

        return safeZone.getDistanceToEdge(location);
    }

    /**
     * Check if a player can enter combat at their current location
     */
    public boolean canEnterCombatAt(Location location) {
        return !isInSafeZone(location);
    }

    // ============ COMBAT TIMER METHODS ============

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
     * Check if player can perform an action while in combat
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

        // Check safe zones (now cuboid-based)
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
     * Show combat status
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

        // Show safe zone status with enhanced info
        SafeZone currentZone = getSafeZone(player.getLocation());
        if (currentZone != null) {
            player.sendMessage(Component.text("🏠 LOCATION: " + currentZone.getName() + " (Safe Zone)")
                    .color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("📐 Zone Size: " + currentZone.getSizeX() + " × " + currentZone.getSizeY() + " × " + currentZone.getSizeZ())
                    .color(NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("🏠 LOCATION: PvP Zone")
                    .color(NamedTextColor.RED));

            double distanceToSafeZone = getDistanceToNearestSafeZone(player.getLocation());
            if (distanceToSafeZone != Double.MAX_VALUE && distanceToSafeZone > 0) {
                player.sendMessage(Component.text("📍 Nearest safe zone: " + (int)distanceToSafeZone + " blocks away")
                        .color(NamedTextColor.GRAY));
            }
        }

        player.sendMessage(Component.text("═══════════════════════════")
                .color(NamedTextColor.GOLD));
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
     * Check if the combat timer system is working properly
     */
    public boolean isSystemHealthy() {
        // Basic health check - could be expanded
        return plugin != null && trustManager != null;
    }

    /**
     * Save all safe zone data
     */
    public void saveAllData() {
        saveSafeZonesToFile();
    }

    /**
     * Debug method to show safe zone coverage at a location
     */
    public void debugSafeZoneAt(Location location, Player player) {
        if (!player.hasPermission("maces.admin")) return;

        SafeZone zone = getSafeZone(location);
        if (zone != null) {
            player.sendMessage(Component.text("DEBUG: Location is in safe zone '" + zone.getName() + "'")
                    .color(NamedTextColor.AQUA));
            player.sendMessage(Component.text("Zone bounds: [" + zone.getMinX() + "," + zone.getMinY() + "," + zone.getMinZ() +
                            " to " + zone.getMaxX() + "," + zone.getMaxY() + "," + zone.getMaxZ() + "]")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Zone size: " + zone.getSizeX() + " × " + zone.getSizeY() + " × " + zone.getSizeZ())
                    .color(NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("DEBUG: Location is NOT in any safe zone")
                    .color(NamedTextColor.RED));

            double nearestDistance = getDistanceToNearestSafeZone(location);
            if (nearestDistance != Double.MAX_VALUE) {
                player.sendMessage(Component.text("Nearest safe zone: " + (int)nearestDistance + " blocks away")
                        .color(NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Check if coordinates are valid for safe zone creation
     */
    public boolean areCoordinatesValid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // Check if coordinates are within reasonable world bounds
        final int MAX_COORDINATE = 29999999;
        final int MIN_COORDINATE = -29999999;
        final int MIN_Y = -64;
        final int MAX_Y = 320;

        // Check X bounds
        if (minX < MIN_COORDINATE || maxX > MAX_COORDINATE) return false;
        // Check Z bounds
        if (minZ < MIN_COORDINATE || maxZ > MAX_COORDINATE) return false;
        // Check Y bounds
        if (minY < MIN_Y || maxY > MAX_Y) return false;

        // Ensure min is actually less than max
        if (minX > maxX || minY > maxY || minZ > maxZ) return false;

        return true;
    }

    /**
     * Calculate volume of a cuboid safe zone
     */
    public long calculateVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}