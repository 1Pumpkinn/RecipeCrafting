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

public class AirmaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public AirmaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "airmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create the actual air mace using MaceManager
        ItemStack result = maceManager.createAirMace();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("GNG", "PHP", "WBW");
        recipe.setIngredient('G', Material.GHAST_TEAR);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('W', Material.WIND_CHARGE);
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
        return "Airmace";
    }

    @Override
    public void onCraft(Player player) {
        // Give the player the air mace directly
        player.getInventory().addItem(maceManager.createAirMace());
        player.sendMessage(Component.text("💨 You crafted an Air Mace!")
                .color(NamedTextColor.GREEN));
    }
}