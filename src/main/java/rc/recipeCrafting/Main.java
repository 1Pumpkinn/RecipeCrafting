package rc.recipeCrafting;

import org.bukkit.plugin.java.JavaPlugin;
import rc.recipeCrafting.listeners.CraftingListener;

public class Main extends JavaPlugin {

    private rc.recipeCrafting.recipes.RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Initialize recipe manager
        recipeManager = new rc.recipeCrafting.recipes.RecipeManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new CraftingListener(this, recipeManager), this);

        // Register all recipes
        recipeManager.registerAllRecipes();

        getLogger().info("CustomCrafting plugin enabled!");
        getLogger().info("Registered " + recipeManager.getRecipeCount() + " custom recipes.");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }
        getLogger().info("CustomCrafting plugin disabled!");
    }

    public rc.recipeCrafting.recipes.RecipeManager getRecipeManager() {
        return recipeManager;
    }
}