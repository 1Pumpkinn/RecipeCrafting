package rc.recipeCrafting.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import rc.recipeCrafting.recipies.CustomRecipe;
import rc.recipeCrafting.recipies.RecipeManager;

public class CraftingListener implements Listener {

    private final JavaPlugin plugin;
    private final RecipeManager recipeManager;

    public CraftingListener(JavaPlugin plugin, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Recipe recipe = event.getRecipe();

        // Check if this is one of our custom recipes
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

            if (recipeManager.isCustomRecipe(shapedRecipe.getKey())) {
                // Cancel the normal crafting
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();

                // Close the player's inventory to prevent issues
                player.closeInventory();

                // Get our custom recipe and execute its craft action
                CustomRecipe customRecipe = recipeManager.getCustomRecipe(shapedRecipe.getKey());
                if (customRecipe != null) {
                    customRecipe.onCraft(player);
                }

                // Remove the ingredients from the crafting matrix
                event.getInventory().setMatrix(new ItemStack[9]);
            }
        }
    }
}