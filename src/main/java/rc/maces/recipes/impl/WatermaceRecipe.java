// WatermaceRecipe.java
package rc.maces.recipes.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.recipes.CustomRecipe;

public class WatermaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public WatermaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "watermace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create the actual water mace using MaceManager
        ItemStack result = maceManager.createWaterMace();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("WCW", "THF", "QBQ");
        recipe.setIngredient('C', Material.CONDUIT);
        recipe.setIngredient('W', Material.TROPICAL_FISH);
        recipe.setIngredient('T', Material.TUBE_CORAL);
        recipe.setIngredient('F', Material.FIRE_CORAL);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('Q', Material.TRIDENT);
        recipe.setIngredient('B', Material.BREEZE_ROD);

        return recipe;
    }

    @Override
    public Recipe getBukkitRecipe() {
        return bukkitRecipe;
    }

    @Override
    public NamespacedKey getRecipeKey() {
        return recipeKey;
    }

    @Override
    public String getRecipeName() {
        return "Watermace";
    }

    @Override
    public void onCraft(Player player) {
        // Give the player the water mace directly
        player.getInventory().addItem(maceManager.createWaterMace());
        player.sendMessage(Component.text("🌊 You crafted a Water Mace!")
                .color(NamedTextColor.BLUE));
    }
}