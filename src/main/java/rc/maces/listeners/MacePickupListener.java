package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
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

        // Check if the item is a custom mace
        if (maceManager.isCustomMace(item)) {
            // Check if player already has any custom mace
            boolean hasAnyMace = false;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (maceManager.isCustomMace(invItem)) {
                    hasAnyMace = true;
                    break;
                }
            }

            // If player already has a mace, cancel pickup
            if (hasAnyMace) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You can only have one mace at a time!")
                        .color(NamedTextColor.RED));
                return;
            }

            // Determine which mace type was picked up and switch element
            String maceElement = null;
            if (maceManager.isAirMace(item)) {
                maceElement = "AIR";
            } else if (maceManager.isFireMace(item)) {
                maceElement = "FIRE";
            } else if (maceManager.isWaterMace(item)) {
                maceElement = "WATER";
            } else if (maceManager.isEarthMace(item)) {
                maceElement = "EARTH";
            }

            if (maceElement != null) {
                // Switch player's element to match the picked up mace
                elementManager.switchElementToMace(player, maceElement);
            }
        }
    }
}