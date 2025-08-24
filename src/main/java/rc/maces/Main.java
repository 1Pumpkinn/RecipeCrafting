package rc.maces;

import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.commands.*;
import rc.maces.listeners.*;
import rc.maces.managers.*;
import rc.maces.recipes.RecipeManager;
import rc.maces.tasks.ActionBarTask;

public class Main extends JavaPlugin {

    private CooldownManager cooldownManager;
    private MaceManager maceManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Initialize managers
        cooldownManager = new CooldownManager();
        maceManager = new MaceManager(this, cooldownManager);
        recipeManager = new RecipeManager(this);

        // Register commands
        getCommand("airmace").setExecutor(new AirmaceCommand(maceManager));
        getCommand("firemace").setExecutor(new FiremaceCommand(maceManager));
        getCommand("watermace").setExecutor(new WatermaceCommand(maceManager));
        getCommand("earthmace").setExecutor(new EarthmaceCommand(maceManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new MaceListener(maceManager), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this, recipeManager), this);

        // Register recipes
        recipeManager.registerAllRecipes();

        // Start action bar task
        new ActionBarTask(maceManager).runTaskTimer(this, 0L, 1L);

        // Start passive effects task
        new PassiveEffectsListener(maceManager).runTaskTimer(this, 0L, 20L); // Every second

        getLogger().info("Maces plugin enabled!");
        getLogger().info("Registered " + recipeManager.getRecipeCount() + " custom recipes.");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }
        getLogger().info("Maces plugin disabled!");
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public MaceManager getMaceManager() {
        return maceManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}