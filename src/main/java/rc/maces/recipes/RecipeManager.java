package rc.maces.recipes;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.recipes.impl.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final JavaPlugin plugin;
    private final Map<NamespacedKey, CustomRecipe> customRecipes;
    private final List<NamespacedKey> registeredKeys;
    private final rc.maces.managers.MaceManager maceManager;

    public RecipeManager(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.customRecipes = new HashMap<>();
        this.registeredKeys = new ArrayList<>();
    }

    public void registerAllRecipes() {
        plugin.getLogger().info("Starting recipe registration...");

        // Register all mace recipes with individual error handling
        registerRecipeWithDelay(new AirmaceRecipe(plugin, maceManager));
        registerRecipeWithDelay(new FiremaceRecipe(plugin, maceManager));
        registerRecipeWithDelay(new WatermaceRecipe(plugin, maceManager));
        registerRecipeWithDelay(new EarthmaceRecipe(plugin, maceManager));

        plugin.getLogger().info("Completed recipe registration. Total recipes: " + getRecipeCount());
    }

    private void registerRecipeWithDelay(CustomRecipe customRecipe) {
        // Add a small delay between recipe registrations to prevent conflicts
        Bukkit.getScheduler().runTaskLater(plugin, () -> registerRecipe(customRecipe), 1L);
    }

    private void registerRecipe(CustomRecipe customRecipe) {
        try {
            // Validate the custom recipe
            if (customRecipe == null) {
                plugin.getLogger().severe("CustomRecipe is null!");
                return;
            }

            if (customRecipe.getBukkitRecipe() == null) {
                plugin.getLogger().severe("BukkitRecipe is null for: " + customRecipe.getRecipeName());
                return;
            }

            if (customRecipe.getRecipeKey() == null) {
                plugin.getLogger().severe("RecipeKey is null for: " + customRecipe.getRecipeName());
                return;
            }

            // Check if recipe already exists
            if (Bukkit.getRecipe(customRecipe.getRecipeKey()) != null) {
                plugin.getLogger().warning("Recipe already exists: " + customRecipe.getRecipeName() + ". Removing old recipe first.");
                Bukkit.removeRecipe(customRecipe.getRecipeKey());
            }

            // Register the bukkit recipe
            boolean success = Bukkit.addRecipe(customRecipe.getBukkitRecipe());

            if (!success) {
                plugin.getLogger().severe("Failed to add recipe to Bukkit: " + customRecipe.getRecipeName());
                return;
            }

            // Store our custom recipe data
            customRecipes.put(customRecipe.getRecipeKey(), customRecipe);
            registeredKeys.add(customRecipe.getRecipeKey());

            plugin.getLogger().info("Successfully registered recipe: " + customRecipe.getRecipeName());

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register recipe: " +
                    (customRecipe != null ? customRecipe.getRecipeName() : "Unknown Recipe"));
            plugin.getLogger().severe("Error details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregisterAllRecipes() {
        plugin.getLogger().info("Starting recipe unregistration...");

        for (NamespacedKey key : new ArrayList<>(registeredKeys)) {
            try {
                boolean removed = Bukkit.removeRecipe(key);
                if (removed) {
                    plugin.getLogger().info("Removed recipe: " + key.getKey());
                } else {
                    plugin.getLogger().warning("Failed to remove recipe: " + key.getKey());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing recipe " + key.getKey() + ": " + e.getMessage());
            }
        }

        customRecipes.clear();
        registeredKeys.clear();
        plugin.getLogger().info("Unregistered all custom recipes.");
    }

    public CustomRecipe getCustomRecipe(NamespacedKey key) {
        return customRecipes.get(key);
    }

    public boolean isCustomRecipe(NamespacedKey key) {
        return customRecipes.containsKey(key);
    }

    public int getRecipeCount() {
        return customRecipes.size();
    }

    // Add method to reload recipes if needed
    public void reloadRecipes() {
        plugin.getLogger().info("Reloading all recipes...");
        unregisterAllRecipes();

        // Wait a bit before re-registering
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            registerAllRecipes();
        }, 5L);
    }
}