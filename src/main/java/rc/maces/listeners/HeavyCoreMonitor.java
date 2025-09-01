package rc.maces.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional security system that monitors heavy core movements
 * and prevents vanilla mace creation through indirect methods
 */
public class HeavyCoreMonitor implements Listener {

    private final JavaPlugin plugin;
    private final Map<Block, Long> recentHeavyCoreActivity = new HashMap<>();

    public HeavyCoreMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    // Monitor heavy core drops near crafters
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item droppedItem = event.getItemDrop();
        ItemStack itemStack = droppedItem.getItemStack();

        if (itemStack.getType() == Material.HEAVY_CORE) {
            // Check for nearby crafters
            checkNearbyBlocks(droppedItem);
        }
    }

    // Monitor heavy core pickups by hoppers/players near crafters
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        ItemStack itemStack = event.getItem().getItemStack();

        if (itemStack.getType() == Material.HEAVY_CORE) {
            checkNearbyBlocks(event.getItem());
        }
    }

    // Monitor inventory interactions involving heavy cores and crafters
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.CRAFTER) {
            ItemStack clicked = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            // Check if heavy core is being moved into crafter
            if (isHeavyCore(clicked) || isHeavyCore(cursor)) {
                Block crafterBlock = null;

                if (event.getInventory().getHolder() instanceof Crafter) {
                    crafterBlock = ((Crafter) event.getInventory().getHolder()).getBlock();
                    recentHeavyCoreActivity.put(crafterBlock, System.currentTimeMillis());

                    // Schedule intensive monitoring
                    scheduleIntensiveMonitoring(crafterBlock);
                }
            }
        }
    }

    /**
     * Check for crafters in a radius around an item
     */
    private void checkNearbyBlocks(Item item) {
        Block center = item.getLocation().getBlock();

        // Check 5x5x5 area around the dropped item
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = center.getRelative(x, y, z);

                    if (block.getType() == Material.CRAFTER) {
                        recentHeavyCoreActivity.put(block, System.currentTimeMillis());
                        scheduleIntensiveMonitoring(block);
                    }
                }
            }
        }
    }

    /**
     * Schedule intensive monitoring for a specific crafter
     */
    private void scheduleIntensiveMonitoring(Block crafterBlock) {
        new BukkitRunnable() {
            int checks = 0;
            final int maxChecks = 100; // Monitor for 10 seconds (every 2 ticks)

            @Override
            public void run() {
                if (checks >= maxChecks || crafterBlock.getType() != Material.CRAFTER) {
                    cancel();
                    return;
                }

                checkCrafterForVanillaMaces(crafterBlock);
                checks++;
            }
        }.runTaskTimer(plugin, 1L, 2L); // Check every 2 ticks for 10 seconds
    }

    /**
     * Intensive crafter checking for vanilla maces
     */
    private void checkCrafterForVanillaMaces(Block crafterBlock) {
        BlockState blockState = crafterBlock.getState();
        if (!(blockState instanceof Crafter)) {
            return;
        }

        Crafter crafter = (Crafter) blockState;
        Inventory inventory = crafter.getInventory();

        // Check all slots
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isVanillaMace(item)) {
                inventory.setItem(i, null);
                plugin.getLogger().warning("SECURITY: Removed vanilla mace from intensively monitored crafter at " +
                        crafterBlock.getLocation());

                // Also check for mace recipe patterns and break them
                breakVanillaMaceRecipe(inventory);
            }
        }
    }

    /**
     * Break vanilla mace recipe patterns in crafter inventory
     */
    private void breakVanillaMaceRecipe(Inventory crafterInventory) {
        // Vanilla mace recipe detection and disruption
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = crafterInventory.getItem(i);
        }

        // Check if this looks like a vanilla mace recipe
        if (looksLikeVanillaMaceRecipe(matrix)) {
            // Disrupt the recipe by removing the heavy core
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && matrix[i].getType() == Material.HEAVY_CORE) {
                    crafterInventory.setItem(i, null);
                    plugin.getLogger().warning("SECURITY: Disrupted vanilla mace recipe pattern in crafter");
                    break;
                }
            }
        }
    }

    /**
     * Check if the crafting matrix looks like a vanilla mace recipe
     */
    private boolean looksLikeVanillaMaceRecipe(ItemStack[] matrix) {
        if (matrix.length != 9) return false;

        // Count heavy cores and breeze rods
        int heavyCores = 0;
        int breezeRods = 0;

        for (ItemStack item : matrix) {
            if (item != null) {
                if (item.getType() == Material.HEAVY_CORE) {
                    heavyCores++;
                } else if (item.getType() == Material.BREEZE_ROD) {
                    breezeRods++;
                }
            }
        }

        // Vanilla mace requires 1 heavy core and 1 breeze rod
        return heavyCores == 1 && breezeRods == 1;
    }

    /**
     * Check if ItemStack is a heavy core
     */
    private boolean isHeavyCore(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE;
    }

    /**
     * Check if ItemStack is a vanilla mace
     */
    private boolean isVanillaMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return true;
        }

        var meta = item.getItemMeta();
        return meta.displayName() == null && (!meta.hasLore() || meta.lore().isEmpty());
    }

    /**
     * Cleanup task to remove old activity records
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                recentHeavyCoreActivity.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > 60000); // Remove after 1 minute
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Run every minute
    }

    /**
     * Get count of actively monitored crafters
     */
    public int getActivelyMonitoredCount() {
        return recentHeavyCoreActivity.size();
    }
}