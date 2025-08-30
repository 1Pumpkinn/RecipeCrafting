package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import rc.maces.managers.CombatTimer;
import rc.maces.managers.TrustManager;

/**
 * Prevents players from using elytras during combat, unless their combat
 * was caused by a trusted ally (preventing abuse while allowing ally interactions)
 */
public class ElytraDisabling implements Listener {

    private final CombatTimer combatTimer;
    private final TrustManager trustManager;

    public ElytraDisabling(CombatTimer combatTimer, TrustManager trustManager) {
        this.combatTimer = combatTimer;
        this.trustManager = trustManager;
    }

    /**
     * Prevent elytra gliding during combat (unless caused by ally)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onElytraToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only prevent if trying to start gliding (not stopping)
        if (!event.isGliding()) {
            return;
        }

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow elytra use
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra use in ally-caused combat
        }

        // Block elytra gliding
        event.setCancelled(true);

        player.sendMessage(Component.text("❌ You cannot use elytra while in combat!")
                .color(NamedTextColor.RED));
        player.sendMessage(Component.text("⚔ Combat ends in: " + combatTimer.getRemainingCombatTimeFormatted(player))
                .color(NamedTextColor.YELLOW));
    }

    /**
     * Prevent equipping elytra during combat by clicking in inventory
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal inventory actions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        // Check if player is trying to equip elytra
        if (isElytraEquipAttempt(event, player)) {
            event.setCancelled(true);

            player.sendMessage(Component.text("❌ You cannot equip elytra while in combat!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("⚔ Combat ends in: " + combatTimer.getRemainingCombatTimeFormatted(player))
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Prevent equipping elytra during combat by dragging in inventory
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal inventory actions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        // Check if dragging elytra to chestplate slot
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && draggedItem.getType() == Material.ELYTRA) {
            // Check if any of the drag slots is the chestplate slot (slot 38 in player inventory)
            if (event.getRawSlots().contains(38)) {
                event.setCancelled(true);

                player.sendMessage(Component.text("❌ You cannot equip elytra while in combat!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("⚔ Combat ends in: " + combatTimer.getRemainingCombatTimeFormatted(player))
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Prevent right-clicking elytra to equip during combat
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal interactions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ELYTRA) {
            // Check if player is trying to right-click equip (and doesn't already have chestplate)
            PlayerInventory inventory = player.getInventory();
            ItemStack chestplate = inventory.getChestplate();

            if (chestplate == null || chestplate.getType() == Material.AIR) {
                event.setCancelled(true);

                player.sendMessage(Component.text("❌ You cannot equip elytra while in combat!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("⚔ Combat ends in: " + combatTimer.getRemainingCombatTimeFormatted(player))
                        .color(NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Additional safety: Prevent switching to hotbar slot containing elytra during combat
     * (This is more of a QoL feature to prevent accidental attempts)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal item switching
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow normal item switching in ally-caused combat
        }

        // Check if switching to elytra
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && newItem.getType() == Material.ELYTRA) {
            // Don't cancel the event, just give a warning
            player.sendMessage(Component.text("⚠ You cannot equip that elytra while in combat!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Helper method to determine if an inventory click is attempting to equip elytra
     */
    private boolean isElytraEquipAttempt(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Check if clicking elytra in inventory to move to chestplate slot
        if (event.getSlot() == 38 && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            // Clicking on chestplate slot
            if (cursorItem != null && cursorItem.getType() == Material.ELYTRA) {
                return true; // Trying to place elytra in chestplate slot
            }
        }

        // Check if clicking elytra to swap with current chestplate
        if (clickedItem != null && clickedItem.getType() == Material.ELYTRA) {
            if (event.getClick().isShiftClick()) {
                // Shift-clicking elytra to auto-equip
                PlayerInventory inventory = player.getInventory();
                ItemStack currentChestplate = inventory.getChestplate();
                if (currentChestplate == null || currentChestplate.getType() == Material.AIR) {
                    return true; // Would auto-equip to empty chestplate slot
                }
            }
        }

        // Check for number key equipping (pressing 1-9 while hovering over chestplate slot)
        if (event.getSlot() == 38 && event.getClick().isKeyboardClick()) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0 && hotbarSlot < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                if (hotbarItem != null && hotbarItem.getType() == Material.ELYTRA) {
                    return true; // Trying to swap elytra from hotbar to chestplate
                }
            }
        }

        return false;
    }

    /**
     * Check if a player is currently wearing elytra (for other systems to use)
     */
    public boolean isWearingElytra(Player player) {
        if (player == null) return false;

        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }

    /**
     * Force remove elytra from a player (for admin commands or special situations)
     */
    public void forceRemoveElytra(Player player, String reason) {
        if (player == null || !isWearingElytra(player)) {
            return;
        }

        ItemStack elytra = player.getInventory().getChestplate();
        player.getInventory().setChestplate(null);

        // Try to add to inventory, drop if full
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(elytra);
        } else {
            player.getWorld().dropItem(player.getLocation(), elytra);
        }

        // Stop gliding if currently gliding
        if (player.isGliding()) {
            player.setGliding(false);
        }

        if (reason != null && !reason.isEmpty()) {
            player.sendMessage(Component.text("🪂 Your elytra has been removed: " + reason)
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Get combat and trust managers (for integration with other systems)
     */
    public CombatTimer getCombatTimer() {
        return combatTimer;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }
}