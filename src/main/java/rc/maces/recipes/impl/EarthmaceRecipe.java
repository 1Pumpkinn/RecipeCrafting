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

public class EarthmaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public EarthmaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "earthmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create the actual earth mace using MaceManager
        ItemStack result = maceManager.createEarthMace();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("MSM", "PHP", "DBD");
        recipe.setIngredient('M', Material.MOSS_BLOCK);
        recipe.setIngredient('S', Material.SCULK_CATALYST);
        recipe.setIngredient('P', Material.PINK_WOOL);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        recipe.setIngredient('D', Material.DEEPSLATE_EMERALD_ORE);

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
        return "Earthmace";
    }

    @Override
    public void onCraft(Player player) {
        // Give the player the earth mace directly
        player.getInventory().addItem(maceManager.createEarthMace());
        player.sendMessage(Component.text("🌍 You crafted an Earth Mace!")
                .color(NamedTextColor.GREEN));
    }
}