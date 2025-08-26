package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.MaceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Comprehensive mace blocking system that prevents vanilla maces from being created
 * through ANY method including crafters, hoppers, dispensers, etc.
 */
public class CraftingRestrictionListener implements Listener {

    private final MaceManager maceManager;
    private final JavaPlugin plugin;
    private final Set<Block> monitoredCrafters = new HashSet<>();

    public CraftingRestrictionListener(MaceManager maceManager, JavaPlugin plugin) {
        this.maceManager = maceManager;
        this.plugin = plugin;

        // Start periodic crafter monitoring
        startCrafterMonitoring();
    }

    // ================== PLAYER CRAFTING PREVENTION ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();

        if (isVanillaMace(result)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use /element to see available elemental maces!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        if (isVanillaMace(result)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Block vanilla maces in result slots
        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack currentItem = event.getCurrentItem();
            if (isVanillaMace(currentItem)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Normal maces are disabled!")
                        .color(NamedTextColor.RED));
                return;
            }
        }

        // Monitor crafter blocks when players interact with them
        if (event.getInventory().getHolder() instanceof Crafter) {
            Block crafterBlock = ((Crafter) event.getInventory().getHolder()).getBlock();
            monitoredCrafters.add(crafterBlock);

            // Schedule immediate check
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkCrafterForVanillaMaces(crafterBlock);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    // ================== HOPPER/DROPPER PREVENTION ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();

        // Block vanilla maces from being moved
        if (isVanillaMace(item)) {
            event.setCancelled(true);
            return;
        }

        // Monitor heavy core movements to crafters
        if (item.getType() == Material.HEAVY_CORE) {
            Inventory destination = event.getDestination();

            if (destination.getHolder() instanceof Crafter) {
                Block crafterBlock = ((Crafter) destination.getHolder()).getBlock();
                monitoredCrafters.add(crafterBlock);

                // Schedule check after movement
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        checkCrafterForVanillaMaces(crafterBlock);
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }

    // ================== DISPENSER PREVENTION ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();

        if (isVanillaMace(item)) {
            event.setCancelled(true);

            // Remove the vanilla mace from the dispenser
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeVanillaMacesFromInventory(event.getBlock().getState());
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    // ================== ITEM SPAWN/DROP PREVENTION ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();

        if (isVanillaMace(itemStack)) {
            // Check if this is from a crafter or other automated source
            if (item.getThrower() == null) { // No player threw this
                event.setCancelled(true);
                plugin.getLogger().warning("Blocked vanilla mace spawn at " +
                        item.getLocation().getBlockX() + ", " +
                        item.getLocation().getBlockY() + ", " +
                        item.getLocation().getBlockZ());
            }
        }
    }

    // ================== CHUNK LOAD MONITORING ==================

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Schedule a check for any crafters in the loaded chunk
        new BukkitRunnable() {
            @Override
            public void run() {
                for (BlockState tileEntity : event.getChunk().getTileEntities()) {
                    if (tileEntity instanceof Crafter) {
                        Block crafterBlock = tileEntity.getBlock();
                        monitoredCrafters.add(crafterBlock);
                        checkCrafterForVanillaMaces(crafterBlock);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    // ================== UTILITY METHODS ==================

    /**
     * Checks if an ItemStack is a vanilla mace (no custom data)
     */
    private boolean isVanillaMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        // If it has no meta, it's definitely vanilla
        if (!item.hasItemMeta()) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();

        // If it has no display name or lore, it's vanilla
        if (meta.displayName() == null && (!meta.hasLore() || meta.lore().isEmpty())) {
            return true;
        }

        return false;
    }

    /**
     * Removes vanilla maces from any inventory
     */
    private void removeVanillaMacesFromInventory(BlockState blockState) {
        if (blockState instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) blockState).getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (isVanillaMace(item)) {
                    inventory.setItem(i, null);
                    plugin.getLogger().info("Removed vanilla mace from " + blockState.getType() +
                            " at " + blockState.getLocation());
                }
            }
        }
    }

    /**
     * Checks a crafter for vanilla maces and removes them
     */
    private void checkCrafterForVanillaMaces(Block crafterBlock) {
        if (crafterBlock.getType() != Material.CRAFTER) {
            monitoredCrafters.remove(crafterBlock);
            return;
        }

        BlockState blockState = crafterBlock.getState();
        if (!(blockState instanceof Crafter)) {
            monitoredCrafters.remove(crafterBlock);
            return;
        }

        Crafter crafter = (Crafter) blockState;
        Inventory inventory = crafter.getInventory();

        // Check result slot (slot 9 in crafter inventory)
        ItemStack result = inventory.getItem(9);
        if (isVanillaMace(result)) {
            inventory.setItem(9, null);
            plugin.getLogger().info("Removed vanilla mace from crafter result slot at " +
                    crafterBlock.getLocation());
        }

        // Check all other slots for vanilla maces that might have been moved in
        for (int i = 0; i < 9; i++) { // Crafting matrix slots
            ItemStack item = inventory.getItem(i);
            if (isVanillaMace(item)) {
                inventory.setItem(i, null);
                plugin.getLogger().info("Removed vanilla mace from crafter crafting slot at " +
                        crafterBlock.getLocation());
            }
        }
    }

    /**
     * Starts the periodic crafter monitoring system
     */
    private void startCrafterMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Create a copy to avoid concurrent modification
                Set<Block> craftersToCheck = new HashSet<>(monitoredCrafters);

                for (Block crafterBlock : craftersToCheck) {
                    checkCrafterForVanillaMaces(crafterBlock);
                }

                // Clean up invalid blocks
                monitoredCrafters.removeIf(block -> block.getType() != Material.CRAFTER);
            }
        }.runTaskTimer(plugin, 20L, 10L); // Check every 0.5 seconds
    }

    /**
     * Public method to force check all known crafters (for admin commands)
     */
    public void forceCheckAllCrafters() {
        Set<Block> craftersToCheck = new HashSet<>(monitoredCrafters);
        for (Block crafterBlock : craftersToCheck) {
            checkCrafterForVanillaMaces(crafterBlock);
        }
    }

    /**
     * Public method to get the number of monitored crafters (for debugging)
     */
    public int getMonitoredCrafterCount() {
        return monitoredCrafters.size();
    }
}