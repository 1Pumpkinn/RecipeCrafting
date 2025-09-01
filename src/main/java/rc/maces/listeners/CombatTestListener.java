package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.managers.CombatTimer;

/**
 * Optional debug listener to test and verify combat timer functionality
 * This can be removed in production or disabled via config
 */
public class CombatTestListener implements Listener {

    private final JavaPlugin plugin;
    private final CombatTimer combatTimer;
    private final boolean debugMode;

    public CombatTestListener(JavaPlugin plugin, CombatTimer combatTimer, boolean debugMode) {
        this.plugin = plugin;
        this.combatTimer = combatTimer;
        this.debugMode = debugMode;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHitPlayer(EntityDamageByEntityEvent event) {
        if (!debugMode) return;

        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Debug logging
        plugin.getLogger().info("PvP Debug: " + attacker.getName() + " hit " + victim.getName());

        // Check if combat timer is working
        boolean attackerInCombat = combatTimer.isInCombat(attacker);
        boolean victimInCombat = combatTimer.isInCombat(victim);

        plugin.getLogger().info("Combat Status - Attacker: " + attackerInCombat + ", Victim: " + victimInCombat);

        // Send debug messages to both players if they have permission
        if (attacker.hasPermission("maces.debug")) {
            attacker.sendMessage(Component.text("DEBUG: Combat status updated")
                    .color(NamedTextColor.GRAY));
        }

        if (victim.hasPermission("maces.debug")) {
            victim.sendMessage(Component.text("DEBUG: Combat status updated")
                    .color(NamedTextColor.GRAY));
        }
    }
}

// Add this to your Main.java in onEnable() if you want debug mode:
/*
// Initialize debug listener (optional)
if (getConfig().getBoolean("debug.combat-timer", false)) {
    CombatTestListener debugListener = new CombatTestListener(this, combatTimer, true);
    getServer().getPluginManager().registerEvents(debugListener, this);
    getLogger().info("Combat timer debug listener enabled!");
}
*/