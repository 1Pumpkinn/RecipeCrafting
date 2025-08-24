package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.managers.ElementManager;
import rc.maces.recipes.CustomRecipe;
import rc.maces.recipes.RecipeManager;

public class CraftingListener implements Listener {

    private final JavaPlugin plugin;
    private final RecipeManager recipeManager;
    private final ElementManager elementManager;

    public CraftingListener(JavaPlugin plugin, RecipeManager recipeManager, ElementManager elementManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.elementManager = elementManager;
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
                Player player = (Player) event.getWhoClicked();
                String playerElement = elementManager.getPlayerElement(player);

                // Check if player can craft this mace based on their element
                String recipeKey = shapedRecipe.getKey().getKey();
                String maceType = getMaceTypeFromRecipeKey(recipeKey);

                if (!elementManager.canCraftMace(player, maceType)) {
                    // Cancel the crafting
                    event.setCancelled(true);

                    player.sendMessage(Component.text("❌ You cannot craft the " + maceType.toLowerCase() + " mace!")
                            .color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Your element is: " + elementManager.getElementDisplayName(playerElement))
                            .color(elementManager.getElementColor(playerElement)));
                    player.sendMessage(Component.text("You can only craft the " + playerElement.toLowerCase() + " mace!")
                            .color(NamedTextColor.GRAY));

                    return;
                }

                // Cancel the normal crafting
                event.setCancelled(true);

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

    /**
     * Extract mace type from recipe key
     */
    private String getMaceTypeFromRecipeKey(String recipeKey) {
        if (recipeKey.contains("airmace")) return "AIR";
        if (recipeKey.contains("firemace")) return "FIRE";
        if (recipeKey.contains("watermace")) return "WATER";
        if (recipeKey.contains("earthmace")) return "EARTH";
        return "UNKNOWN";
    }
}