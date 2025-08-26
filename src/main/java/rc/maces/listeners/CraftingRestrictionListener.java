package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Comprehensive mace blocking system that prevents vanilla maces from being created
 * through ANY method including crafters, hoppers, dispensers, etc.
 * Also prevents elemental maces from being crafted in crafter blocks.
 */
public class CraftingRestrictionListener implements Listener {

    private final MaceManager maceManager;
    private final JavaPlugin plugin;
    private final Set<Block> monitoredCrafters = new HashSet<>();

    // List of elemental mace recipe keys to block in crafters
    private final List<String> elementalMaceRecipes = Arrays.asList(
            "airmace_recipe",
            "firemace_recipe",
            "watermace_recipe",
            "earthmace_recipe"
    );

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

        // Block vanilla mace crafting everywhere
        if (isVanillaMace(result)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use /element to see available elemental maces!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Block elemental mace crafting in crafter blocks
        if (event.getInventory().getType() == InventoryType.CRAFTER) {
            Recipe recipe = event.getRecipe();
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                String recipeKey = shapedRecipe.getKey().getKey();

                if (elementalMaceRecipes.contains(recipeKey)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ Elemental maces cannot be crafted in crafter blocks!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Please use a regular crafting table instead!")
                            .color(NamedTextColor.YELLOW));
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        // Block vanilla maces from appearing in result slots
        if (isVanillaMace(result)) {
            event.getInventory().setResult(null);
            return;
        }

        // Block elemental maces from appearing in crafter result slots
        if (event.getInventory().getType() == InventoryType.CRAFTER && event.getRecipe() instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) event.getRecipe();
            String recipeKey = shapedRecipe.getKey().getKey();

            if (elementalMaceRecipes.contains(recipeKey)) {
                event.getInventory().setResult(null);
            }
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

            // Block elemental maces in crafter result slots
            if (event.getInventory().getType() == InventoryType.CRAFTER && isElementalMace(currentItem)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Elemental maces cannot be crafted in crafter blocks!")
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
                    checkCrafterForProhibitedMaces(crafterBlock);
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

        // Block elemental maces from being moved into crafters
        if (isElementalMace(item) && event.getDestination().getHolder() instanceof Crafter) {
            event.setCancelled(true);
            return;
        }

        // Monitor heavy core movements to crafters (for vanilla mace prevention)
        if (item.getType() == Material.HEAVY_CORE) {
            Inventory destination = event.getDestination();

            if (destination.getHolder() instanceof Crafter) {
                Block crafterBlock = ((Crafter) destination.getHolder()).getBlock();
                monitoredCrafters.add(crafterBlock);

                // Schedule check after movement
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        checkCrafterForProhibitedMaces(crafterBlock);
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }

    // ================== DISPENSER PREVENTION ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();

        if (isVanillaMace(item) || isElementalMace(item)) {
            event.setCancelled(true);

            // Remove the prohibited mace from the dispenser
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeProhibitedMacesFromInventory(event.getBlock().getState());
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
        } else if (isElementalMace(itemStack)) {
            // Check if this elemental mace came from a crafter (shouldn't happen)
            if (item.getThrower() == null) {
                // Check if there's a crafter nearby that might have created this
                Block nearbyBlock = item.getLocation().getBlock();
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            Block checkBlock = nearbyBlock.getRelative(x, y, z);
                            if (checkBlock.getType() == Material.CRAFTER) {
                                event.setCancelled(true);
                                plugin.getLogger().warning("Blocked elemental mace spawn from crafter at " +
                                        item.getLocation().getBlockX() + ", " +
                                        item.getLocation().getBlockY() + ", " +
                                        item.getLocation().getBlockZ());
                                return;
                            }
                        }
                    }
                }
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
                        checkCrafterForProhibitedMaces(crafterBlock);
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
     * Checks if an ItemStack is an elemental mace (custom mace)
     */
    private boolean isElementalMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        return maceManager.isCustomMace(item);
    }

    /**
     * Removes prohibited maces from any inventory
     */
    private void removeProhibitedMacesFromInventory(BlockState blockState) {
        if (blockState instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) blockState).getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (isVanillaMace(item) || (isElementalMace(item) && blockState instanceof Crafter)) {
                    inventory.setItem(i, null);
                    plugin.getLogger().info("Removed prohibited mace from " + blockState.getType() +
                            " at " + blockState.getLocation());
                }
            }
        }
    }

    /**
     * Checks a crafter for prohibited maces and removes them
     */
    private void checkCrafterForProhibitedMaces(Block crafterBlock) {
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
        if (isVanillaMace(result) || isElementalMace(result)) {
            inventory.setItem(9, null);
            plugin.getLogger().info("Removed prohibited mace from crafter result slot at " +
                    crafterBlock.getLocation());
        }

        // Check all other slots for prohibited maces
        for (int i = 0; i < 9; i++) { // Crafting matrix slots
            ItemStack item = inventory.getItem(i);
            if (isVanillaMace(item) || isElementalMace(item)) {
                inventory.setItem(i, null);
                plugin.getLogger().info("Removed prohibited mace from crafter crafting slot at " +
                        crafterBlock.getLocation());
            }
        }

        // Also check for elemental mace recipe patterns and disrupt them
        checkAndDisruptElementalMacePatterns(inventory, crafterBlock);
    }

    /**
     * Check for and disrupt elemental mace recipe patterns in crafter
     */
    private void checkAndDisruptElementalMacePatterns(Inventory crafterInventory, Block crafterBlock) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = crafterInventory.getItem(i);
        }

        // Check if this looks like any elemental mace recipe
        if (looksLikeElementalMaceRecipe(matrix)) {
            // Disrupt the recipe by removing a key component (heavy core first, then breeze rod)
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && matrix[i].getType() == Material.HEAVY_CORE) {
                    crafterInventory.setItem(i, null);
                    plugin.getLogger().warning("SECURITY: Disrupted elemental mace recipe pattern (removed Heavy Core) in crafter at " + crafterBlock.getLocation());
                    return;
                }
            }
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && matrix[i].getType() == Material.BREEZE_ROD) {
                    crafterInventory.setItem(i, null);
                    plugin.getLogger().warning("SECURITY: Disrupted elemental mace recipe pattern (removed Breeze Rod) in crafter at " + crafterBlock.getLocation());
                    return;
                }
            }
        }
    }

    /**
     * Check if the crafting matrix looks like an elemental mace recipe
     */
    private boolean looksLikeElementalMaceRecipe(ItemStack[] matrix) {
        if (matrix.length != 9) return false;

        // All elemental maces require heavy core and breeze rod
        boolean hasHeavyCore = false;
        boolean hasBreezeRod = false;

        for (ItemStack item : matrix) {
            if (item != null) {
                if (item.getType() == Material.HEAVY_CORE) {
                    hasHeavyCore = true;
                } else if (item.getType() == Material.BREEZE_ROD) {
                    hasBreezeRod = true;
                }
            }
        }

        return hasHeavyCore && hasBreezeRod;
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
                    checkCrafterForProhibitedMaces(crafterBlock);
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
            checkCrafterForProhibitedMaces(crafterBlock);
        }
    }

    /**
     * Public method to get the number of monitored crafters (for debugging)
     */
    public int getMonitoredCrafterCount() {
        return monitoredCrafters.size();
    }
}