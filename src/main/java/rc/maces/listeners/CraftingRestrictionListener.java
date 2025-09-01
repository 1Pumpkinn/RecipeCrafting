package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
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
 * Simplified mace blocking system that prevents vanilla and elemental maces
 * from being crafted through ANY method.
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
        startCrafterMonitoring();
    }

    // ================== PREVENT ALL CRAFTING ==================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();

        // Block vanilla mace crafting everywhere
        if (isVanillaMace(result)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Use /element to see available elemental maces!")
                        .color(NamedTextColor.YELLOW));
            }
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
                    if (event.getWhoClicked() instanceof Player) {
                        Player player = (Player) event.getWhoClicked();
                        player.sendMessage(Component.text("❌ Elemental maces cannot be crafted in crafter blocks!")
                                .color(NamedTextColor.RED));
                        player.sendMessage(Component.text("Please use a regular crafting table instead!")
                                .color(NamedTextColor.YELLOW));
                    }
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        // Remove vanilla maces from result slots
        if (isVanillaMace(result)) {
            event.getInventory().setResult(null);
            return;
        }

        // Remove elemental maces from crafter result slots
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
        // Block taking items from result slots
        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack currentItem = event.getCurrentItem();

            if (isVanillaMace(currentItem)) {
                event.setCancelled(true);
                return;
            }

            if (event.getInventory().getType() == InventoryType.CRAFTER && isElementalMace(currentItem)) {
                event.setCancelled(true);
                return;
            }
        }

        // Monitor crafter blocks
        if (event.getInventory().getHolder() instanceof Crafter) {
            Block crafterBlock = ((Crafter) event.getInventory().getHolder()).getBlock();
            monitoredCrafters.add(crafterBlock);
        }
    }

    // ================== PREVENT AUTOMATED SYSTEMS ==================

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
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();

        if (isVanillaMace(item) || isElementalMace(item)) {
            event.setCancelled(true);
        }
    }

    // ================== CHUNK MONITORING ==================

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (BlockState tileEntity : event.getChunk().getTileEntities()) {
                        if (tileEntity instanceof Crafter) {
                            Block crafterBlock = tileEntity.getBlock();
                            monitoredCrafters.add(crafterBlock);
                            checkCrafterForProhibitedMaces(crafterBlock);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking crafters in chunk: " + e.getMessage());
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    // ================== UTILITY METHODS ==================

    private boolean isVanillaMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.displayName() == null && (!meta.hasLore() || meta.lore().isEmpty())) {
            return true;
        }

        return false;
    }

    private boolean isElementalMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }
        return maceManager.isCustomMace(item);
    }

    private void checkCrafterForProhibitedMaces(Block crafterBlock) {
        try {
            if (crafterBlock == null || crafterBlock.getType() != Material.CRAFTER) {
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

            if (inventory == null || inventory.getSize() < 9) {
                monitoredCrafters.remove(crafterBlock);
                return;
            }

            // Clear result slot if it has prohibited items
            if (inventory.getSize() >= 10) {
                ItemStack result = inventory.getItem(9);
                if (isVanillaMace(result) || isElementalMace(result)) {
                    inventory.setItem(9, null);
                }
            }

            // Check crafting matrix and disrupt prohibited patterns
            checkAndDisruptProhibitedPatterns(inventory, crafterBlock);

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking crafter at " +
                    (crafterBlock != null ? crafterBlock.getLocation() : "unknown") + ": " + e.getMessage());
            if (crafterBlock != null) {
                monitoredCrafters.remove(crafterBlock);
            }
        }
    }

    private void checkAndDisruptProhibitedPatterns(Inventory crafterInventory, Block crafterBlock) {
        try {
            if (crafterInventory.getSize() < 9) return;

            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                matrix[i] = crafterInventory.getItem(i);
            }

            // Check for vanilla mace pattern (heavy core + breeze rod)
            if (hasVanillaMacePattern(matrix)) {
                // Remove heavy core to disrupt vanilla mace crafting
                for (int i = 0; i < 9; i++) {
                    if (matrix[i] != null && matrix[i].getType() == Material.HEAVY_CORE) {
                        crafterInventory.setItem(i, null);
                        plugin.getLogger().info("Disrupted vanilla mace recipe in crafter at " + crafterBlock.getLocation());
                        return;
                    }
                }
            }

            // Check for elemental mace patterns
            if (hasElementalMacePattern(matrix)) {
                // Remove breeze rod to disrupt elemental mace crafting in crafter
                for (int i = 0; i < 9; i++) {
                    if (matrix[i] != null && matrix[i].getType() == Material.BREEZE_ROD) {
                        crafterInventory.setItem(i, null);
                        plugin.getLogger().info("Disrupted elemental mace recipe in crafter at " + crafterBlock.getLocation());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking patterns at " + crafterBlock.getLocation() + ": " + e.getMessage());
        }
    }

    private boolean hasVanillaMacePattern(ItemStack[] matrix) {
        if (matrix == null || matrix.length != 9) return false;

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

    private boolean hasElementalMacePattern(ItemStack[] matrix) {
        // Same as vanilla for now since both use heavy core + breeze rod
        return hasVanillaMacePattern(matrix);
    }

    private void startCrafterMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Set<Block> craftersToCheck = new HashSet<>(monitoredCrafters);

                    for (Block crafterBlock : craftersToCheck) {
                        try {
                            checkCrafterForProhibitedMaces(crafterBlock);
                        } catch (Exception e) {
                            monitoredCrafters.remove(crafterBlock);
                        }
                    }

                    // Clean up invalid blocks
                    monitoredCrafters.removeIf(block -> {
                        try {
                            return block.getType() != Material.CRAFTER;
                        } catch (Exception e) {
                            return true;
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Critical error in crafter monitoring: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    // ================== PUBLIC METHODS ==================

    public void forceCheckAllCrafters() {
        Set<Block> craftersToCheck = new HashSet<>(monitoredCrafters);
        for (Block crafterBlock : craftersToCheck) {
            try {
                checkCrafterForProhibitedMaces(crafterBlock);
            } catch (Exception e) {
                plugin.getLogger().warning("Error in forced crafter check: " + e.getMessage());
            }
        }
    }

    public int getMonitoredCrafterCount() {
        return monitoredCrafters.size();
    }
}