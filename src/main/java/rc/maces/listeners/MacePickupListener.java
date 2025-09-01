package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

public class MacePickupListener implements Listener {

    private final MaceManager maceManager;
    private final ElementManager elementManager;

    public MacePickupListener(MaceManager maceManager, ElementManager elementManager) {
        this.maceManager = maceManager;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();

        if (maceManager.isCustomMace(item)) {
            // Count existing maces in inventory
            int maceCount = countMaces(player);

            if (maceCount >= 1) {
                // Cancel pickup and notify
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
            }

            // Always change element to match the mace
            String playerElement = elementManager.getPlayerElement(player);
            String maceElement = getMaceElement(item);

            if (maceElement != null && !maceElement.equals(playerElement)) {
                elementManager.setPlayerElement(player, maceElement);

                // Send only one clean message
                player.sendMessage(Component.text("⚡ Your element has changed to " +
                                elementManager.getElementDisplayName(maceElement) + "!")
                        .color(elementManager.getElementColor(maceElement)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Skip if player is in creative mode (let them do whatever)
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // FIXED: Better logic for inventory click handling
        boolean isPlacingMace = cursorItem != null && maceManager.isCustomMace(cursorItem);
        boolean isMovingMace = clickedItem != null && maceManager.isCustomMace(clickedItem);

        // Handle placing mace into player inventory
        if (isPlacingMace && event.getClickedInventory() != null &&
                event.getClickedInventory().equals(player.getInventory())) {

            int currentMaceCount = countMaces(player);

            // Allow if they're replacing the current mace (swap)
            if (isMovingMace) {
                return; // This is a swap, allow it
            }

            // Block if they already have a mace and are trying to add another
            if (currentMaceCount >= 1) {
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
            }
        }

        // FIXED: Handle shift-clicking maces from containers (chests, etc.)
        if (event.isShiftClick() && isMovingMace) {
            // Check if clicking from a container (not player inventory) to player inventory
            if (event.getClickedInventory() != null &&
                    !event.getClickedInventory().equals(player.getInventory()) &&
                    isContainerInventory(event.getClickedInventory().getType())) {

                int currentMaceCount = countMaces(player);
                if (currentMaceCount >= 1) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                            .color(NamedTextColor.YELLOW));
                    return;
                }
            }
        }

        // FIXED: Handle taking maces from containers by clicking
        if (isMovingMace && event.getClickedInventory() != null &&
                !event.getClickedInventory().equals(player.getInventory()) &&
                isContainerInventory(event.getClickedInventory().getType())) {

            int currentMaceCount = countMaces(player);
            if (currentMaceCount >= 1) {
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
            }
        }

        // Handle element switching when successfully picking up maces
        if ((isPlacingMace || (event.isShiftClick() && isMovingMace)) && !event.isCancelled()) {
            ItemStack maceItem = isPlacingMace ? cursorItem : clickedItem;
            handleElementSwitch(player, maceItem);
        }
    }

    /**
     * ADDED: Handle inventory dragging to prevent bypassing limits
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Skip if player is in creative mode
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && maceManager.isCustomMace(draggedItem)) {
            // Check if any of the drag slots are in the player's inventory
            boolean draggingToPlayerInventory = event.getRawSlots().stream()
                    .anyMatch(slot -> slot < player.getInventory().getSize());

            if (draggingToPlayerInventory) {
                int currentMaceCount = countMaces(player);
                if (currentMaceCount >= 1) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                            .color(NamedTextColor.YELLOW));
                    return;
                }
            }
        }
    }

    /**
     * ADDED: Handle inventory close to prevent the ESC bypass exploit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Skip if player is in creative mode
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        // Check if player has cursor item (item being dragged when they closed)
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && maceManager.isCustomMace(cursorItem)) {
            int currentMaceCount = countMaces(player);

            // If they already have a mace and are trying to add another via cursor
            if (currentMaceCount >= 1) {
                // Drop the cursor item at their location instead of adding to inventory
                player.getWorld().dropItemNaturally(player.getLocation(), cursorItem);
                player.setItemOnCursor(null);

                // Use scheduler to send message after inventory is fully closed
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(Component.text("⚠️ You can only carry one mace at a time! Mace dropped.")
                                .color(NamedTextColor.YELLOW));
                    }
                }.runTaskLater(maceManager.getPlugin(), 1L);

                return;
            }

            // If they don't have a mace yet, allow them to keep it and switch element
            handleElementSwitch(player, cursorItem);
        }

        // ADDED: Additional check for inventory contents after close
        // This catches any edge cases where items might have been duplicated
        new BukkitRunnable() {
            @Override
            public void run() {
                enforceOneMetalLimit(player);
            }
        }.runTaskLater(maceManager.getPlugin(), 2L);
    }

    /**
     * ADDED: Enforce one mace limit by removing excess maces
     */
    private void enforceOneMetalLimit(Player player) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack firstMace = null;
        int maceCount = 0;
        boolean removedAny = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && maceManager.isCustomMace(item)) {
                maceCount++;

                if (firstMace == null) {
                    firstMace = item; // Keep the first mace found
                } else {
                    // Remove excess maces
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    contents[i] = null;
                    removedAny = true;
                }
            }
        }

        if (removedAny) {
            player.getInventory().setContents(contents);
            player.sendMessage(Component.text("⚠️ Excess maces removed! You can only carry one mace at a time.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * ADDED: Check if inventory type is a container that players can access
     */
    private boolean isContainerInventory(InventoryType type) {
        switch (type) {
            case CHEST:
            case ENDER_CHEST:
            case SHULKER_BOX:
            case BARREL:
            case HOPPER:
            case DROPPER:
            case DISPENSER:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case BREWING:
            case MERCHANT:
            case CREATIVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * ADDED: Handle element switching logic
     */
    private void handleElementSwitch(Player player, ItemStack maceItem) {
        String maceElement = getMaceElement(maceItem);
        if (maceElement != null) {
            String playerElement = elementManager.getPlayerElement(player);
            if (!maceElement.equals(playerElement)) {
                elementManager.setPlayerElement(player, maceElement);

                // Use scheduler to send message after inventory operations complete
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(Component.text("⚡ Your element has changed to " +
                                        elementManager.getElementDisplayName(maceElement) + "!")
                                .color(elementManager.getElementColor(maceElement)));
                    }
                }.runTaskLater(maceManager.getPlugin(), 1L);
            }
        }
    }

    private int countMaces(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && maceManager.isCustomMace(item)) {
                count++;
            }
        }
        return count;
    }

    // Helper method to get element from mace type
    private String getMaceElement(ItemStack mace) {
        if (maceManager.isAirMace(mace)) {
            return "AIR";
        } else if (maceManager.isFireMace(mace)) {
            return "FIRE";
        } else if (maceManager.isWaterMace(mace)) {
            return "WATER";
        } else if (maceManager.isEarthMace(mace)) {
            return "EARTH";
        }
        return null;
    }
}