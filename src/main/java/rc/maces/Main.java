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
    private TrustManager trustManager;

    @Override
    public void onEnable() {
        // Initialize managers
        cooldownManager = new CooldownManager();
        elementManager = new ElementManager(this);
        maceManager = new MaceManager(this, cooldownManager);
        recipeManager = new RecipeManager(this, maceManager);
        trustManager = new TrustManager(this);

        // Register mace commands
        getCommand("airmace").setExecutor(new AirmaceCommand(maceManager));
        getCommand("firemace").setExecutor(new FiremaceCommand(maceManager));
        getCommand("watermace").setExecutor(new WatermaceCommand(maceManager));
        getCommand("earthmace").setExecutor(new EarthmaceCommand(maceManager));

        // Register element commands
        getCommand("element").setExecutor(new ElementCommand(elementManager));
        getCommand("reroll").setExecutor(new RerollCommand(elementManager));
        getCommand("myelement").setExecutor(new MyElementCommand(elementManager));
        getCommand("craftedmaces").setExecutor(new CraftedMacesCommand(elementManager, maceManager));

        // Register trust commands
        getCommand("trust").setExecutor(new TrustCommand(trustManager));
        getCommand("untrust").setExecutor(new UntrustCommand(trustManager));
        getCommand("trustlist").setExecutor(new TrustListCommand(trustManager));
        getCommand("trustaccept").setExecutor(new TrustAcceptCommand(trustManager));
        getCommand("trustdeny").setExecutor(new TrustDenyCommand(trustManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(
                new MaceListener(maceManager, elementManager, trustManager), this);
        getServer().getPluginManager().registerEvents(
                new CraftingListener(this, recipeManager, elementManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(elementManager), this);

        // Register recipes
        recipeManager.registerAllRecipes();

        // Start action bar task
        new ActionBarTask(maceManager).runTaskTimer(this, 0L, 1L);

        // Start passive effects task
        new PassiveEffectsListener(maceManager, elementManager, trustManager)
                .runTaskTimer(this, 0L, 20L); // Every second

        // Plugin startup messages
        getLogger().info("Maces plugin enabled!");
        getLogger().info("Registered " + recipeManager.getRecipeCount() + " custom recipes.");
        getLogger().info("Element system initialized - players will be assigned random elements on join!");
        getLogger().info("Trust system initialized - players can now form alliances!");
        getLogger().info("FEATURES ENABLED:");
        getLogger().info("- Element-based passive abilities for all players");
        getLogger().info("- Enhanced mace abilities with cooldowns");
        getLogger().info("- Trust/alliance system with PvP protection");
        getLogger().info("- Element-restricted crafting system");
        getLogger().info("- Real-time ability status display");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }
        if (elementManager != null) {
            elementManager.saveAllData();
        }
        if (trustManager != null) {
            trustManager.saveAllData();
        }
        getLogger().info("Maces plugin disabled!");
    }

    // Getters for managers
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

    public TrustManager getTrustManager() {
        return trustManager;
    }
}