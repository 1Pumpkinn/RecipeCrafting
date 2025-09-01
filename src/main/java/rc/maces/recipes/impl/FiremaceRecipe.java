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

public class FiremaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public FiremaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "firemace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create the actual fire mace using MaceManager
        ItemStack result = maceManager.createFireMace();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("LWL", "LHL", "DBD");
        recipe.setIngredient('L', Material.LAVA_BUCKET);
        recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        recipe.setIngredient('D', Material.DRIED_GHAST);

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
        return "Firemace";
    }

    @Override
    public void onCraft(Player player) {
        // Give the player the fire mace directly
        player.getInventory().addItem(maceManager.createFireMace());
        player.sendMessage(Component.text("🔥 You crafted a Fire Mace!")
                .color(NamedTextColor.RED));
    }
}