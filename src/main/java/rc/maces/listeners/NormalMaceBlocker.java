package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from crafting normal vanilla maces
 * Only allows custom elemental maces to be crafted
 */
public class NormalMaceBlocker implements Listener {

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        // Check if they're trying to craft a normal mace
        if (result.getType() == Material.MACE) {
            // Check if this is NOT one of our custom recipes by checking the display name
            if (!result.hasItemMeta() || result.getItemMeta().displayName() == null) {
                // This is a vanilla mace recipe
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Use /element to see available elemental maces!")
                        .color(NamedTextColor.YELLOW));
                return;
            }
        }
    }
}