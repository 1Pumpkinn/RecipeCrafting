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

public class AirmaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public AirmaceRecipe(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "airmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create placeholder item (diamond pickaxe with custom name)
        ItemStack result = new ItemStack(Material.MACE, 1);
        var meta = result.getItemMeta();
        meta.setDisplayName("§bAirmace Placeholder");
        result.setItemMeta(meta);

        // Create the shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Set the pattern
        recipe.shape("GNG", "PHP", "WBW");

        // Set the ingredients
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
        // Execute the airmace command through console
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "airmace " + player.getName());
        });

        plugin.getLogger().info("Airmace crafted by " + player.getName());
    }
}