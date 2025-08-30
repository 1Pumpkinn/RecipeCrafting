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
    private CombatTimer combatTimer;
    private PassiveEffectsListener passiveEffectsListener;
    private CraftingListener craftingListener;
    private CraftingRestrictionListener craftingRestrictionListener;
    private HeavyCoreMonitor heavyCoreMonitor;
    private MovementPreventionListener movementPreventionListener;
    private CombatCommandBlocker combatCommandBlocker;
    private ElytraDisabling elytraDisabling;

    @Override
    public void onEnable() {
        // Initialize managers in correct order
        cooldownManager = new CooldownManager();
        elementManager = new ElementManager(this);
        trustManager = new TrustManager(this);

        // Initialize combat timer after trust manager
        combatTimer = new CombatTimer(this, trustManager);

        // Initialize elytra disabling after both combatTimer and trustManager exist
        elytraDisabling = new ElytraDisabling(combatTimer, trustManager, this);

        maceManager = new MaceManager(this, cooldownManager, trustManager);
        recipeManager = new RecipeManager(this, maceManager);

        // Initialize crafting listener (needs to be done before command registration)
        craftingListener = new CraftingListener(this, recipeManager, elementManager);

        // Initialize security listeners
        craftingRestrictionListener = new CraftingRestrictionListener(maceManager, this);
        heavyCoreMonitor = new HeavyCoreMonitor(this);

        // Initialize movement prevention listener
        movementPreventionListener = new MovementPreventionListener();

        // Initialize combat command blocker
        combatCommandBlocker = new CombatCommandBlocker(combatTimer);

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
        getCommand("macestatus").setExecutor(new MaceStatusCommand(craftingListener));

        // Register combat command
        CombatCommand combatCommand = new CombatCommand(combatTimer);
        getCommand("combat").setExecutor(combatCommand);

        // Register safe zone command
        getCommand("safezone").setExecutor(new SafeZoneCommand(combatTimer));

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

        // Register combat timer listener (CRITICAL for combat logging)
        getServer().getPluginManager().registerEvents(
                combatTimer, this);

        // Register ElytraDisabling listener (CRITICAL for elytra blocking)
        getServer().getPluginManager().registerEvents(
                elytraDisabling, this);

        // Register combat command blocker
        getServer().getPluginManager().registerEvents(
                combatCommandBlocker, this);

        // Link combat command with command blocker
        combatCommand.setCommandBlocker(combatCommandBlocker);

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

        // Start automatic mace scanning (every 5 minutes)
        getServer().getScheduler().runTaskLater(this, () -> {
            // Create a ScanMacesCommand instance to start auto-scanning
            ScanMacesCommand scanCommand = new ScanMacesCommand(craftingListener, this);

            // Start auto scanning after server has fully loaded
            getServer().getScheduler().runTaskTimer(this, () -> {
                craftingListener.scanAllPlayersCommand();
                getLogger().info("Automatic mace scan completed for all online players");
            }, 100L, 6000L); // Start after 5 seconds, then every 5 minutes (6000 ticks)

            getLogger().info("Automatic mace scanning enabled - runs every 5 minutes");
        }, 100L); // 5 second delay to let server fully load

        // Plugin startup messages
        getLogger().info("Maces plugin enabled!");
        getLogger().info("Registered " + recipeManager.getRecipeCount() + " custom recipes.");
        elytraDisabling.startElytraMonitoring(); // Start elytra monitoring
        getLogger().info("Combat timer system enabled!");
        getLogger().info("Elytra blocking system enabled!");
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

    public CombatTimer getCombatTimer() {
        return combatTimer;
    }

    public PassiveEffectsListener getPassiveEffectsListener() {
        return passiveEffectsListener;
    }

    public ElytraDisabling getElytraDisabling() {
        return elytraDisabling;
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

    public CombatCommandBlocker getCombatCommandBlocker() {
        return combatCommandBlocker;
    }
}