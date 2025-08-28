package rc.maces;

import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.commands.*;
import rc.maces.commands.ScanMacesCommand;
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
    private PassiveEffectsListener passiveEffectsListener;
    private CraftingListener craftingListener;
    private CraftingRestrictionListener craftingRestrictionListener;
    private HeavyCoreMonitor heavyCoreMonitor;
    private MovementPreventionListener movementPreventionListener;

    @Override
    public void onEnable() {
        // Initialize managers in correct order (TrustManager before MaceManager)
        cooldownManager = new CooldownManager();
        elementManager = new ElementManager(this);
        trustManager = new TrustManager(this);
        maceManager = new MaceManager(this, cooldownManager, trustManager);
        recipeManager = new RecipeManager(this, maceManager);

        // Initialize crafting listener (needs to be done before command registration)
        craftingListener = new CraftingListener(this, recipeManager, elementManager);

        // Initialize security listeners
        craftingRestrictionListener = new CraftingRestrictionListener(maceManager, this);
        heavyCoreMonitor = new HeavyCoreMonitor(this);

        // Initialize movement prevention listener
        movementPreventionListener = new MovementPreventionListener();

        // Register mace commands
        getCommand("airmace").setExecutor(new AirmaceCommand(maceManager));
        getCommand("firemace").setExecutor(new FiremaceCommand(maceManager));
        getCommand("watermace").setExecutor(new WatermaceCommand(maceManager));
        getCommand("earthmace").setExecutor(new EarthmaceCommand(maceManager));

        // Register element commands
        getCommand("element").setExecutor(new ElementCommand(elementManager));
        getCommand("reroll").setExecutor(new RerollCommand(elementManager));
        getCommand("myelement").setExecutor(new MyElementCommand(elementManager, maceManager));
        getCommand("craftedmaces").setExecutor(new CraftedMacesCommand(elementManager, maceManager, craftingListener));

        // Register reset command
        getCommand("resetmaces").setExecutor(new ResetMaceCommand(elementManager, maceManager, craftingListener));
        getCommand("scanmaces").setExecutor(new ScanMacesCommand(craftingListener, this));

        // Register trust commands
        getCommand("trust").setExecutor(new TrustCommand(trustManager));
        getCommand("untrust").setExecutor(new UntrustCommand(trustManager));
        getCommand("trustlist").setExecutor(new TrustListCommand(trustManager));
        getCommand("trustaccept").setExecutor(new TrustAcceptCommand(trustManager));
        getCommand("trustdeny").setExecutor(new TrustDenyCommand(trustManager));

        // Register trust debug command (admin only)
        getCommand("trustdebug").setExecutor(new TrustDebugCommand(trustManager));

        // Register security command
        getCommand("macesecurity").setExecutor(new MaceSecurityCommand(craftingRestrictionListener, heavyCoreMonitor));

        // Register event listeners
        getServer().getPluginManager().registerEvents(
                new MaceListener(maceManager, elementManager, trustManager), this);
        getServer().getPluginManager().registerEvents(
                craftingListener, this);
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(elementManager), this);
        getServer().getPluginManager().registerEvents(
                new MacePickupListener(maceManager, elementManager), this);

        getServer().getPluginManager().registerEvents(
                craftingRestrictionListener, this);
        getServer().getPluginManager().registerEvents(
                heavyCoreMonitor, this);

        // Register movement prevention listener (CRITICAL for Vine Trap)
        getServer().getPluginManager().registerEvents(
                movementPreventionListener, this);

        // Register recipes
        recipeManager.registerAllRecipes();

        // Start action bar task
        new ActionBarTask(maceManager).runTaskTimer(this, 0L, 1L);

        // Create and store PassiveEffectsListener reference
        passiveEffectsListener = new PassiveEffectsListener(maceManager, elementManager, trustManager);
        passiveEffectsListener.runTaskTimer(this, 0L, 20L); // Every second

        // Clean up any orphaned trust relationships on startup
        getServer().getScheduler().runTaskLater(this, () -> {
            trustManager.cleanupOrphanedTrusts();
        }, 20L); // Run after 1 second delay

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
        getLogger().info("- Auto element switching when picking up maces");
        getLogger().info("- One mace per player limit enforced");
        getLogger().info("- Normal mace crafting disabled");
        getLogger().info("- Chat spam prevention active");
        getLogger().info("- Mace crafting reset system enabled");
        getLogger().info("- Advanced mace security system active");
        getLogger().info("- Movement prevention system active");
        getLogger().info("- Trust system debug tools available (/trustdebug)");
        getLogger().info("- Automatic mace scanning available (/scanmaces auto)");
    }

    @Override
    public void onDisable() {
        // Stop automatic scanning if running
        ScanMacesCommand.stopAutoScan();

        if (recipeManager != null) {
            recipeManager.unregisterAllRecipes();
        }
        if (elementManager != null) {
            elementManager.saveAllData();
        }
        if (trustManager != null) {
            trustManager.saveAllData();
        }

        if (craftingListener != null) {
            craftingListener.onDisable();
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

    public PassiveEffectsListener getPassiveEffectsListener() {
        return passiveEffectsListener;
    }

    public CraftingListener getCraftingListener() {
        return craftingListener;
    }

    public CraftingRestrictionListener getCraftingRestrictionListener() {
        return craftingRestrictionListener;
    }

    public HeavyCoreMonitor getHeavyCoreMonitor() {
        return heavyCoreMonitor;
    }

    public MovementPreventionListener getMovementPreventionListener() {
        return movementPreventionListener;
    }
}