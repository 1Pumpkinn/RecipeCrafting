package rc.maces.recipes;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;

public interface CustomRecipe {

    /**
     * Gets the Bukkit recipe object to register
     */
    Recipe getBukkitRecipe();

    /**
     * Gets the unique key for this recipe
     */
    NamespacedKey getRecipeKey();

    /**
     * Gets the display name of this recipe
     */
    String getRecipeName();

    /**
     * Called when a player crafts this recipe
     * @param player The player who crafted the item
     */
    void onCraft(Player player);
}