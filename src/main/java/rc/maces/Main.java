package rc.maces;

import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.commands.*;
import rc.maces.listeners.*;
import rc.maces.managers.*;
import rc.maces.recipes.RecipeManager;
import rc.maces.tasks.ActionBarTask;

public class Main extends JavaPlugin {

    private CooldownManager cooldownManager;
    private ElementManager elementManager;
    private MaceManager maceManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Initialize managers
        cooldownManager = new CooldownManager();
        elementManager = new ElementManager(this);
        maceManager = new MaceManager(this, cooldownManager);
        recipeManager = new RecipeManager(this);

        // Register commands
        getCommand("airmace").setExecutor(new AirmaceCommand(maceManager));
        getCommand("firemace").setExecutor(new FiremaceCommand(maceManager));
        getCommand("watermace").setExecutor(new WatermaceCommand(maceManager));
        getCommand("earthmace").setExecutor(new EarthmaceCommand(maceManager));
        getCommand("element").setExecutor(new ElementCommand(elementManager));
        getCommand("reroll").setExecutor(new RerollCommand(elementManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new MaceListener(maceManager, elementManager), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this, recipeManager, elementManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(elementManager), this);

        // Register recipes
        recipeManager.registerAllRecipes();

        // Start action bar task
        new ActionBarTask(maceManager).runTaskTimer(this, 0L, 1L);

        // Start passive effects task
        new PassiveEffectsListener(maceManager, elementManager).runTaskTimer(this, 0L, 20L); // Every second

        getLogger().info("Maces plugin enabled!");
        getLogger().info("Registered " + recipeManager.getRecipeCount() + " custom recipes.");
        getLogger().info("Element system initialized - players will be assigned random elements on join!");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }
        if (elementManager != null) {
            elementManager.saveAllData();
        }
        getLogger().info("Maces plugin disabled!");
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ElementManager getElementManager() {
        return elementManager;
    }

    public MaceManager getMaceManager() {
        return maceManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}