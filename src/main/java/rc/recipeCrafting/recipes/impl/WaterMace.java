package rc.recipeCrafting.recipes.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import rc.recipeCrafting.recipes.CustomRecipe;

public class WaterMace implements CustomRecipe {

    private final JavaPlugin plugin;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public WaterMace(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "watermace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create placeholder item (mace with custom name)
        ItemStack result = new ItemStack(Material.MACE, 1);
        var meta = result.getItemMeta();
        meta.setDisplayName("§9Water Mace Placeholder");
        result.setItemMeta(meta);

        // Create the shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Set the pattern - Water mace
        recipe.shape("FCF", "WHR", "TBT");

        // Set the ingredients (fixed duplicate 'F' definitions)
        recipe.setIngredient('F', Material.TROPICAL_FISH_BUCKET);
        recipe.setIngredient('C', Material.CONDUIT);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('W', Material.TUBE_CORAL);
        recipe.setIngredient('R', Material.FIRE_CORAL);
        recipe.setIngredient('T', Material.TRIDENT);
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
        // Execute the watermace command through console
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "watermace " + player.getName());
        });

        plugin.getLogger().info("Water mace crafted by " + player.getName());
    }
}