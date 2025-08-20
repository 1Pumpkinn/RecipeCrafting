package rc.recipeCrafting;

import org.bukkit.plugin.java.JavaPlugin;
import rc.recipeCrafting.listeners.CraftingListener;
import rc.recipeCrafting.recipies.RecipeManager;

public class Main extends JavaPlugin {

    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Initialize recipe manager
        recipeManager = new RecipeManager(this);

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

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}