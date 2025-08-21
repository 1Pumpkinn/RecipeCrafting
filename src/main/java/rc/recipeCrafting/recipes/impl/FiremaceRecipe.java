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

public class FiremaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final NamespacedKey recipeKey;
    private final ShapedRecipe bukkitRecipe;

    public FiremaceRecipe(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "firemace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        // Create placeholder item (mace with custom name)
        ItemStack result = new ItemStack(Material.MACE, 1);
        var meta = result.getItemMeta();
        meta.setDisplayName("§cFiremace Placeholder");
        result.setItemMeta(meta);

        // Create the shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Set the pattern - Fire mace (Image 2: lava bucket, coal block, lava bucket, lava bucket, observer, lava bucket, cobblestone, stick, cobblestone)
        recipe.shape("LWL", "LHL", "DBD");

        // Set the ingredients
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
        // Execute the firemace command through console
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "firemace " + player.getName());
        });

        plugin.getLogger().info("Firemace crafted by " + player.getName());
    }
}