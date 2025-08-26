package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();

        if (maceManager.isCustomMace(item)) {
            // Count existing maces in inventory
            int maceCount = countMaces(player);

            if (maceCount >= 1) {
                // Cancel pickup and drop the mace
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
            }

            // FIXED: Always change element to match the mace being picked up (no restrictions)
            String playerElement = elementManager.getPlayerElement(player);
            String maceElement = getMaceElement(item);

            if (maceElement != null && !maceElement.equals(playerElement)) {
                elementManager.setPlayerElement(player, maceElement);
                player.sendMessage(Component.text("⚡ Your element has changed to " +
                                elementManager.getElementDisplayName(maceElement) + "!")
                        .color(elementManager.getElementColor(maceElement)));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Check if player is trying to move a mace into their inventory
        if (clickedItem != null && maceManager.isCustomMace(clickedItem)) {
            // Check if they already have a mace
            if (countMaces(player) >= 1 && !event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
            }
        }

        if (cursorItem != null && maceManager.isCustomMace(cursorItem)) {
            if (countMaces(player) >= 1 && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("⚠️ You can only carry one mace at a time!")
                        .color(NamedTextColor.YELLOW));
                return;
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