package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
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

    // Track how many of each mace type each player has crafted
    private final Map<UUID, Map<String, Integer>> playerMaceCounts = new HashMap<>();
    private File maceDataFile;
    private FileConfiguration maceDataConfig;

    private static final int MAX_MACES_PER_TYPE = 1;

    public CraftingListener(JavaPlugin plugin, RecipeManager recipeManager, ElementManager elementManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.elementManager = elementManager;

        // Initialize persistent storage
        initializeMaceDataFile();
        loadMaceData();
    }

    @EventHandler
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

                // Check if player has already crafted this mace type
                int currentCount = getPlayerMaceCount(player.getUniqueId(), maceType);
                if (currentCount >= MAX_MACES_PER_TYPE) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ You can only craft " + MAX_MACES_PER_TYPE + " " +
                                    maceType.toLowerCase() + " mace!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("You have already crafted: " + currentCount + "/" + MAX_MACES_PER_TYPE)
                            .color(NamedTextColor.GRAY));
                    return;
                }

                // Cancel the normal crafting and handle it ourselves
                event.setCancelled(true);

                // Get our custom recipe and execute its craft action
                CustomRecipe customRecipe = recipeManager.getCustomRecipe(shapedRecipe.getKey());
                if (customRecipe != null) {
                    customRecipe.onCraft(player);

                    // Increment the mace count and save
                    incrementPlayerMaceCount(player.getUniqueId(), maceType);
                    saveMaceData();

                    player.sendMessage(Component.text("✅ " + maceType.toLowerCase() + " mace crafted! (" +
                                    (currentCount + 1) + "/" + MAX_MACES_PER_TYPE + ")")
                            .color(NamedTextColor.GREEN));
                }

                // Remove the ingredients from the crafting matrix
                event.getInventory().setMatrix(new ItemStack[9]);
            }
        }
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
    }

    /**
     * Save mace data to file
     */
    private void saveMaceData() {
        // Clear existing data
        maceDataConfig.set("players", null);

        // Save current data
        for (Map.Entry<UUID, Map<String, Integer>> playerEntry : playerMaceCounts.entrySet()) {
            String playerId = playerEntry.getKey().toString();
            for (Map.Entry<String, Integer> maceEntry : playerEntry.getValue().entrySet()) {
                maceDataConfig.set("players." + playerId + "." + maceEntry.getKey(), maceEntry.getValue());
            }
        }

        try {
            maceDataConfig.save(maceDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace_data.yml: " + e.getMessage());
        }
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
     * Increment the count of a specific mace type for a player
     */
    private void incrementPlayerMaceCount(UUID playerId, String maceType) {
        Map<String, Integer> playerCounts = playerMaceCounts.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCounts.put(maceType, playerCounts.getOrDefault(maceType, 0) + 1);
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
}