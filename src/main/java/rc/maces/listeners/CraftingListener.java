package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.ElementManager;
import rc.maces.recipes.CustomRecipe;
import rc.maces.recipes.RecipeManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftingListener implements Listener {

    private final JavaPlugin plugin;
    private final RecipeManager recipeManager;
    private final ElementManager elementManager;

    // Track how many of each mace type each player has crafted (for display purposes)
    private final Map<UUID, Map<String, Integer>> playerMaceCounts = new HashMap<>();

    // NEW: Track global crafting status - only one of each mace can be crafted server-wide
    private final Map<String, Boolean> globalMaceCrafted = new HashMap<>();
    private final Map<String, String> globalMaceCrafters = new HashMap<>(); // Track who crafted each mace

    private File maceDataFile;
    private FileConfiguration maceDataConfig;

    private static final int MAX_MACES_PER_TYPE_GLOBAL = 1; // Server-wide limit

    public CraftingListener(JavaPlugin plugin, RecipeManager recipeManager, ElementManager elementManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.elementManager = elementManager;

        // Initialize global crafting status
        globalMaceCrafted.put("AIR", false);
        globalMaceCrafted.put("FIRE", false);
        globalMaceCrafted.put("WATER", false);
        globalMaceCrafted.put("EARTH", false);

        // Initialize persistent storage
        initializeMaceDataFile();
        loadMaceData();

        // Scan all online players for existing maces
        scanAllPlayersForMaces();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Scan this player's inventory for maces after a short delay to ensure they're fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                scanPlayerForMaces(player);
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Recipe recipe = event.getRecipe();

        // Check if this is one of our custom recipes
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

            if (recipeManager.isCustomRecipe(shapedRecipe.getKey())) {
                Player player = (Player) event.getWhoClicked();

                // PREVENT CRAFTING IN CRAFTERS - Only allow crafting table
                if (event.getInventory().getType() == InventoryType.CRAFTER) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ Elemental maces can only be crafted in a crafting table!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Use a regular crafting table, not a crafter block!")
                            .color(NamedTextColor.YELLOW));
                    return;
                }

                String playerElement = elementManager.getPlayerElement(player);

                // Check if player can craft this mace based on their element
                String recipeKey = shapedRecipe.getKey().getKey();
                String maceType = getMaceTypeFromRecipeKey(recipeKey);

                if (!elementManager.canCraftMace(player, maceType)) {
                    // Cancel the crafting
                    event.setCancelled(true);

                    player.sendMessage(Component.text("❌ You cannot craft the " + maceType.toLowerCase() + " mace!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Your element is: " + elementManager.getElementDisplayName(playerElement))
                            .color(elementManager.getElementColor(playerElement)));
                    player.sendMessage(Component.text("You can only craft the " + playerElement.toLowerCase() + " mace!")
                            .color(NamedTextColor.GRAY));

                    return;
                }

                // NEW: Check if this mace type has already been crafted server-wide
                if (globalMaceCrafted.getOrDefault(maceType, false)) {
                    event.setCancelled(true);
                    String crafter = globalMaceCrafters.getOrDefault(maceType, "Unknown");
                    player.sendMessage(Component.text("❌ The " + maceType.toLowerCase() + " mace has already been crafted!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Crafted by: " + crafter)
                            .color(NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Only one of each mace type can exist on the server!")
                            .color(NamedTextColor.YELLOW));
                    return;
                }

                // Cancel the normal crafting and handle it ourselves
                event.setCancelled(true);

                // Get our custom recipe and execute its craft action
                CustomRecipe customRecipe = recipeManager.getCustomRecipe(shapedRecipe.getKey());
                if (customRecipe != null) {
                    customRecipe.onCraft(player);

                    // NEW: Mark this mace type as crafted globally
                    globalMaceCrafted.put(maceType, true);
                    globalMaceCrafters.put(maceType, player.getName());

                    // Increment the player's personal count (for display)
                    incrementPlayerMaceCount(player.getUniqueId(), maceType);
                    saveMaceData();

                    // Success messages
                    player.sendMessage(Component.text("✅ " + maceType.toLowerCase() + " mace crafted!")
                            .color(NamedTextColor.GREEN));

                    // Announce to server
                    Component announcement = Component.text("🔥 " + player.getName() + " has crafted the " + maceType.toLowerCase() + " mace!")
                            .color(NamedTextColor.LIGHT_PURPLE);

                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(player)) {
                            onlinePlayer.sendMessage(announcement);
                        }
                    }

                    plugin.getLogger().info("GLOBAL MACE CRAFTED: " + player.getName() + " crafted the " + maceType + " mace");
                }

                // Remove the ingredients from the crafting matrix
                event.getInventory().setMatrix(new ItemStack[9]);
            }
        }
    }

    /**
     * Scan all online players for existing maces
     */
    private void scanAllPlayersForMaces() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int scannedCount = 0;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player != null && player.isOnline()) {
                            scanPlayerForMaces(player);
                            scannedCount++;
                        }
                    }
                    plugin.getLogger().info("Completed mace scanning for " + scannedCount + " online players.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during mass player scanning: " + e.getMessage());
                }
            }
        }.runTaskLater(plugin, 40L); // 2 second delay to let server fully load
    }

    /**
     * Scan a specific player's inventory and ender chest for maces
     */
    private void scanPlayerForMaces(Player player) {
        try {
            Map<String, Integer> foundMaces = new HashMap<>();

            // Scan player's main inventory with null checks
            ItemStack[] inventoryContents = player.getInventory().getContents();
            if (inventoryContents != null) {
                for (ItemStack item : inventoryContents) {
                    if (item != null) {
                        String maceType = identifyMaceType(item);
                        if (maceType != null) {
                            foundMaces.put(maceType, foundMaces.getOrDefault(maceType, 0) + item.getAmount());
                        }
                    }
                }
            }

            // Scan player's ender chest with null checks
            ItemStack[] enderChestContents = player.getEnderChest().getContents();
            if (enderChestContents != null) {
                for (ItemStack item : enderChestContents) {
                    if (item != null) {
                        String maceType = identifyMaceType(item);
                        if (maceType != null) {
                            foundMaces.put(maceType, foundMaces.getOrDefault(maceType, 0) + item.getAmount());
                        }
                    }
                }
            }

            // Update the player's mace counts and global status based on what we found
            boolean updated = false;
            UUID playerId = player.getUniqueId();

            for (Map.Entry<String, Integer> entry : foundMaces.entrySet()) {
                String maceType = entry.getKey();
                int foundCount = Math.min(entry.getValue(), 1); // Cap at 1 since only one can exist
                int currentCount = getPlayerMaceCount(playerId, maceType);

                if (foundCount > currentCount) {
                    setPlayerMaceCount(playerId, maceType, foundCount);

                    // NEW: Mark as globally crafted if found
                    if (foundCount > 0) {
                        globalMaceCrafted.put(maceType, true);
                        // Only update crafter if not already set
                        if (!globalMaceCrafters.containsKey(maceType)) {
                            globalMaceCrafters.put(maceType, player.getName());
                        }
                    }

                    updated = true;
                    plugin.getLogger().info("Updated " + player.getName() + "'s " + maceType.toLowerCase() +
                            " mace count from " + currentCount + " to " + foundCount);
                }
            }

            if (updated) {
                saveMaceData();
            }
        } catch (Exception e) {
            // Catch any exceptions to prevent console spam
            plugin.getLogger().warning("Failed to scan player " + player.getName() + " for maces: " + e.getMessage());
        }
    }

    /**
     * Identify what type of mace an ItemStack is
     * This method needs to be customized based on how your maces are identified
     */
    private String identifyMaceType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            }

            // Check display name first
            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                if (displayName != null) {
                    String displayNameLower = displayName.toLowerCase();
                    if (displayNameLower.contains("air mace")) return "AIR";
                    if (displayNameLower.contains("fire mace")) return "FIRE";
                    if (displayNameLower.contains("water mace")) return "WATER";
                    if (displayNameLower.contains("earth mace")) return "EARTH";
                }
            }

            // Check lore if display name doesn't match
            if (meta.hasLore() && meta.getLore() != null) {
                for (String lore : meta.getLore()) {
                    if (lore != null) {
                        String loreLower = lore.toLowerCase();
                        if (loreLower.contains("air mace")) return "AIR";
                        if (loreLower.contains("fire mace")) return "FIRE";
                        if (loreLower.contains("water mace")) return "WATER";
                        if (loreLower.contains("earth mace")) return "EARTH";

                        // Alternative: check for elemental keywords
                        if (loreLower.contains("elemental") && loreLower.contains("air")) return "AIR";
                        if (loreLower.contains("elemental") && loreLower.contains("fire")) return "FIRE";
                        if (loreLower.contains("elemental") && loreLower.contains("water")) return "WATER";
                        if (loreLower.contains("elemental") && loreLower.contains("earth")) return "EARTH";
                    }
                }
            }

        } catch (Exception e) {
            // Silently handle any exceptions to prevent console spam
            return null;
        }

        return null;
    }

    /**
     * Command to manually scan all online players (for admin use)
     */
    public void scanAllPlayersCommand() {
        scanAllPlayersForMaces();
    }

    /**
     * Scan a player when they come online (deferred scanning)
     */
    public void scanPlayerDeferred(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                scanPlayerForMaces(player);
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    /**
     * Extract mace type from recipe key
     */
    private String getMaceTypeFromRecipeKey(String recipeKey) {
        if (recipeKey.contains("airmace")) return "AIR";
        if (recipeKey.contains("firemace")) return "FIRE";
        if (recipeKey.contains("watermace")) return "WATER";
        if (recipeKey.contains("earthmace")) return "EARTH";
        return "UNKNOWN";
    }

    /**
     * Initialize the mace data file for persistent storage
     */
    private void initializeMaceDataFile() {
        maceDataFile = new File(plugin.getDataFolder(), "mace_data.yml");
        if (!maceDataFile.exists()) {
            try {
                maceDataFile.getParentFile().mkdirs();
                maceDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mace_data.yml file: " + e.getMessage());
            }
        }
        maceDataConfig = YamlConfiguration.loadConfiguration(maceDataFile);
    }

    /**
     * Load mace data from file
     */
    private void loadMaceData() {
        // Load player mace counts
        if (maceDataConfig.contains("players")) {
            for (String playerIdString : maceDataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdString);
                    Map<String, Integer> maceCounts = new HashMap<>();

                    for (String maceType : maceDataConfig.getConfigurationSection("players." + playerIdString).getKeys(false)) {
                        int count = maceDataConfig.getInt("players." + playerIdString + "." + maceType);
                        maceCounts.put(maceType, count);
                    }

                    playerMaceCounts.put(playerId, maceCounts);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in mace_data.yml: " + playerIdString);
                }
            }
        }

        // NEW: Load global crafting status
        if (maceDataConfig.contains("global")) {
            for (String maceType : maceDataConfig.getConfigurationSection("global").getKeys(false)) {
                boolean crafted = maceDataConfig.getBoolean("global." + maceType + ".crafted", false);
                String crafter = maceDataConfig.getString("global." + maceType + ".crafter", "Unknown");

                globalMaceCrafted.put(maceType, crafted);
                if (crafted) {
                    globalMaceCrafters.put(maceType, crafter);
                }
            }
        }

        plugin.getLogger().info("Loaded mace data - Global status: " +
                "Air: " + (globalMaceCrafted.get("AIR") ? "Crafted by " + globalMaceCrafters.get("AIR") : "Available") + ", " +
                "Fire: " + (globalMaceCrafted.get("FIRE") ? "Crafted by " + globalMaceCrafters.get("FIRE") : "Available") + ", " +
                "Water: " + (globalMaceCrafted.get("WATER") ? "Crafted by " + globalMaceCrafters.get("WATER") : "Available") + ", " +
                "Earth: " + (globalMaceCrafted.get("EARTH") ? "Crafted by " + globalMaceCrafters.get("EARTH") : "Available"));
    }

    /**
     * Save mace data to file
     */
    public void saveMaceData() {
        // Clear existing data
        maceDataConfig.set("players", null);
        maceDataConfig.set("global", null);

        // Save player mace counts
        for (Map.Entry<UUID, Map<String, Integer>> playerEntry : playerMaceCounts.entrySet()) {
            String playerId = playerEntry.getKey().toString();
            for (Map.Entry<String, Integer> maceEntry : playerEntry.getValue().entrySet()) {
                maceDataConfig.set("players." + playerId + "." + maceEntry.getKey(), maceEntry.getValue());
            }
        }

        // NEW: Save global crafting status
        for (Map.Entry<String, Boolean> entry : globalMaceCrafted.entrySet()) {
            String maceType = entry.getKey();
            maceDataConfig.set("global." + maceType + ".crafted", entry.getValue());
            if (entry.getValue() && globalMaceCrafters.containsKey(maceType)) {
                maceDataConfig.set("global." + maceType + ".crafter", globalMaceCrafters.get(maceType));
            }
        }

        try {
            maceDataConfig.save(maceDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace_data.yml: " + e.getMessage());
        }
    }

    /**
     * Called when the plugin is being disabled - saves all data
     */
    public void onDisable() {
        saveMaceData();
        plugin.getLogger().info("Mace data saved successfully on plugin shutdown.");
    }

    /**
     * Get the current count of a specific mace type for a player
     */
    private int getPlayerMaceCount(UUID playerId, String maceType) {
        return playerMaceCounts
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .getOrDefault(maceType, 0);
    }

    /**
     * Set the count of a specific mace type for a player
     */
    private void setPlayerMaceCount(UUID playerId, String maceType, int count) {
        Map<String, Integer> playerCounts = playerMaceCounts.computeIfAbsent(playerId, k -> new HashMap<>());
        if (count <= 0) {
            playerCounts.remove(maceType);
            if (playerCounts.isEmpty()) {
                playerMaceCounts.remove(playerId);
            }
        } else {
            playerCounts.put(maceType, count);
        }
    }

    /**
     * Increment the count of a specific mace type for a player
     */
    private void incrementPlayerMaceCount(UUID playerId, String maceType) {
        Map<String, Integer> playerCounts = playerMaceCounts.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCounts.put(maceType, playerCounts.getOrDefault(maceType, 0) + 1);
    }

    /**
     * NEW: Reset global mace status (admin command)
     */
    public void resetGlobalMaceStatus(String maceType) {
        globalMaceCrafted.put(maceType, false);
        globalMaceCrafters.remove(maceType);
        saveMaceData();
    }

    /**
     * NEW: Reset all global mace statuses (admin command)
     */
    public void resetAllGlobalMaceStatuses() {
        globalMaceCrafted.replaceAll((k, v) -> false);
        globalMaceCrafters.clear();
        saveMaceData();
    }

    /**
     * NEW: Manually set a mace as crafted by a specific player
     */
    public void setGlobalMaceCrafted(String maceType, String playerName) {
        globalMaceCrafted.put(maceType, true);
        globalMaceCrafters.put(maceType, playerName);
        saveMaceData();
        plugin.getLogger().info("MANUAL OVERRIDE: " + maceType + " mace set as crafted by " + playerName);
    }

    /**
     * Reset mace count for a specific type for a player
     */
    public void resetPlayerMaceCount(UUID playerId, String maceType) {
        Map<String, Integer> playerCounts = playerMaceCounts.get(playerId);
        if (playerCounts != null) {
            playerCounts.remove(maceType);
            if (playerCounts.isEmpty()) {
                playerMaceCounts.remove(playerId);
            }
            saveMaceData();
        }
    }

    /**
     * Reset all mace counts for a player
     */
    public void resetAllPlayerMaceCounts(UUID playerId) {
        playerMaceCounts.remove(playerId);
        saveMaceData();
    }

    /**
     * Get all mace counts for a player (for admin/debug purposes)
     */
    public Map<String, Integer> getPlayerMaceCounts(UUID playerId) {
        return new HashMap<>(playerMaceCounts.getOrDefault(playerId, new HashMap<>()));
    }

    /**
     * NEW: Get global mace crafting status (for admin commands)
     */
    public Map<String, Boolean> getGlobalMaceCraftedStatus() {
        return new HashMap<>(globalMaceCrafted);
    }

    /**
     * NEW: Get global mace crafters (for admin commands)
     */
    public Map<String, String> getGlobalMaceCrafters() {
        return new HashMap<>(globalMaceCrafters);
    }

    /**
     * NEW: Check if a specific mace type has been crafted globally
     */
    public boolean isGlobalMaceCrafted(String maceType) {
        return globalMaceCrafted.getOrDefault(maceType, false);
    }

    /**
     * NEW: Get who crafted a specific mace type
     */
    public String getGlobalMaceCrafter(String maceType) {
        return globalMaceCrafters.getOrDefault(maceType, "Unknown");
    }
}