package rc.recipeCrafting.recipies;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import rc.recipeCrafting.recipies.impl.AirmaceRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final JavaPlugin plugin;
    private final Map<NamespacedKey, CustomRecipe> customRecipes;
    private final List<NamespacedKey> registeredKeys;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.customRecipes = new HashMap<>();
        this.registeredKeys = new ArrayList<>();
    }

    public void registerAllRecipes() {
        // Register all your custom recipes here
        registerRecipe(new AirmaceRecipe(plugin));

        // Add more recipes here as you create them:
        // registerRecipe(new FireSwordRecipe(plugin));
        // registerRecipe(new IceBowRecipe(plugin));
        // etc.
    }

    private void registerRecipe(CustomRecipe customRecipe) {
        try {
            // Register the bukkit recipe
            Bukkit.addRecipe(customRecipe.getBukkitRecipe());

            // Store our custom recipe data
            customRecipes.put(customRecipe.getRecipeKey(), customRecipe);
            registeredKeys.add(customRecipe.getRecipeKey());

            plugin.getLogger().info("Registered recipe: " + customRecipe.getRecipeName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register recipe: " + customRecipe.getRecipeName());
            e.printStackTrace();
        }
    }

    public void unregisterAllRecipes() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
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
}