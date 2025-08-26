package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.managers.MaceManager;

/**
 * Simplified crafting restriction listener that only prevents vanilla maces
 * The CraftingListener handles all custom mace restrictions and tracking
 */
public class CraftingRestrictionListener implements Listener {

    private final MaceManager maceManager;
    private final JavaPlugin plugin;

    public CraftingRestrictionListener(MaceManager maceManager, JavaPlugin plugin) {
        this.maceManager = maceManager;
        this.plugin = plugin;
    }

    // Handle all player crafting (crafting table, inventory crafting)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();

        if (result.getType() == Material.MACE) {
            // Check if this is a vanilla mace (no custom name/lore)
            if (!result.hasItemMeta() ||
                    result.getItemMeta().displayName() == null ||
                    !result.getItemMeta().hasLore()) {

                // This is a vanilla mace recipe - block it completely
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Use /element to see available elemental maces!")
                        .color(NamedTextColor.YELLOW));
                return;
            }
        }
    }

    // Handle preparation in crafting interfaces (prevents showing recipe)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        if (result != null && result.getType() == Material.MACE) {
            // Check if this is a vanilla mace
            if (!result.hasItemMeta() ||
                    result.getItemMeta().displayName() == null ||
                    !result.getItemMeta().hasLore()) {

                // Block vanilla mace recipe from showing
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    // Handle hopper transfers that might bypass crafting restrictions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();

        if (item.getType() == Material.MACE) {
            // Check if this is a vanilla mace being moved
            if (!item.hasItemMeta() ||
                    item.getItemMeta().displayName() == null ||
                    !item.getItemMeta().hasLore()) {

                // Block movement of vanilla maces
                event.setCancelled(true);
            }
        }
    }

    // Handle clicking in crafting result slots
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if clicking in result slot of crafting inventory
        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack currentItem = event.getCurrentItem();

            if (currentItem != null && currentItem.getType() == Material.MACE) {
                // Check if vanilla mace
                if (!currentItem.hasItemMeta() ||
                        currentItem.getItemMeta().displayName() == null ||
                        !currentItem.getItemMeta().hasLore()) {

                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ Normal maces are disabled!")
                            .color(NamedTextColor.RED));
                    return;
                }
            }
        }
    }
}