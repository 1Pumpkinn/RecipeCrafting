package rc.maces.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import rc.maces.managers.CombatTimer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener to automatically remove spawn protection when players perform certain actions
 * This should be registered alongside your CombatTimer
 */
public class SpawnProtectionListener implements Listener {

    private final CombatTimer combatTimer;
    private final Map<UUID, Location> spawnLocations = new HashMap<>();
    private static final double MAX_SPAWN_DISTANCE = 50.0; // blocks from spawn before protection is removed

    public SpawnProtectionListener(CombatTimer combatTimer) {
        this.combatTimer = combatTimer;
    }

    /**
     * Store spawn location when player joins for distance tracking
     */
    public void setPlayerSpawnLocation(Player player, Location spawnLocation) {
        spawnLocations.put(player.getUniqueId(), spawnLocation.clone());
    }

    /**
     * Remove spawn protection when player moves too far from spawn
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only check if player has spawn protection
        if (!combatTimer.hasSpawnProtection(player)) {
            return;
        }

        // Check if player moved significantly (not just head movement)
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        // Check distance from spawn location
        Location spawnLoc = spawnLocations.get(player.getUniqueId());
        if (spawnLoc != null && spawnLoc.getWorld().equals(to.getWorld())) {
            double distance = spawnLoc.distance(to);
            if (distance > MAX_SPAWN_DISTANCE) {
                combatTimer.removeSpawnProtection(player, "moved " + (int)distance + " blocks from spawn");
                spawnLocations.remove(player.getUniqueId()); // Clean up
            }
        }
    }

    /**
     * Remove spawn protection when player attacks someone
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();

        if (combatTimer.hasSpawnProtection(attacker)) {
            // Don't remove protection if the attack was cancelled (e.g., attacking an ally)
            if (!event.isCancelled()) {
                combatTimer.removeSpawnProtection(attacker, "initiated combat");
                spawnLocations.remove(attacker.getUniqueId()); // Clean up
            }
        }
    }

    /**
     * Remove spawn protection when player uses certain commands that could be considered "active play"
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!combatTimer.hasSpawnProtection(player) || event.isCancelled()) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Commands that should remove spawn protection
        if (command.startsWith("/tp") || command.startsWith("/teleport") ||
                command.startsWith("/home") || command.startsWith("/warp") ||
                command.startsWith("/back") || command.startsWith("/spawn")) {

            combatTimer.removeSpawnProtection(player, "used teleport command");
            spawnLocations.remove(player.getUniqueId());
        }
    }

    /**
     * Remove spawn protection when player opens certain inventories (like enderchest)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!combatTimer.hasSpawnProtection(player) || event.isCancelled()) {
            return;
        }

        // Remove protection when accessing storage that could contain valuable items
        String inventoryTitle = event.getView().getTitle().toLowerCase();
        if (inventoryTitle.contains("ender chest") || inventoryTitle.contains("shulker")) {
            combatTimer.removeSpawnProtection(player, "accessed protected storage");
            spawnLocations.remove(player.getUniqueId());
        }
    }

    /**
     * Clean up spawn location tracking when player quits
     */
    public void onPlayerQuit(Player player) {
        spawnLocations.remove(player.getUniqueId());
    }

    /**
     * Get the maximum distance from spawn before protection is removed
     */
    public static double getMaxSpawnDistance() {
        return MAX_SPAWN_DISTANCE;
    }

    /**
     * Manual method to remove spawn protection for admins
     */
    public void forceRemoveSpawnProtection(Player player, String reason) {
        combatTimer.removeSpawnProtection(player, reason);
        spawnLocations.remove(player.getUniqueId());
    }
}