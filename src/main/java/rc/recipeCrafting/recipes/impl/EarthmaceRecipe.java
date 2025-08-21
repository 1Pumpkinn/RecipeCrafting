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

public class EarthmaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public EarthmaceRecipe(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "earthmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create placeholder item (mace with custom name)
        ItemStack result = new ItemStack(Material.MACE, 1);
        var meta = result.getItemMeta();
        meta.setDisplayName("§aEarthmace Placeholder");
        result.setItemMeta(meta);

        // Create the shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Set the pattern - Earth mace (Image 4: moss block, sculk sensor, moss block, cherry wood, observer, cherry wood, sculk vein, stick, sculk vein)
        recipe.shape("MSM", "PHP", "EBE");

        // Set the ingredients
        recipe.setIngredient('M', Material.MOSS_BLOCK);
        recipe.setIngredient('S', Material.SCULK_CATALYST);
        recipe.setIngredient('P', Material.PINK_WOOL);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('E', Material.DEEPSLATE_EMERALD_ORE);
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
        return "Earthmace";
    }

    @Override
    public void onCraft(Player player) {
        // Execute the earthmace command through console
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "earthmace " + player.getName());
        });

        plugin.getLogger().info("Earthmace crafted by " + player.getName());
    }
}