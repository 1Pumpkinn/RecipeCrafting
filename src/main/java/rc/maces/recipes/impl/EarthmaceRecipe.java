package rc.maces.recipes.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private ShapedRecipe bukkitRecipe;

    public EarthmaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "earthmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        try {
            // Create the actual earth mace using MaceManager
            ItemStack result = maceManager.createEarthMace();

            if (result == null || result.getType() == Material.AIR) {
                plugin.getLogger().severe("EarthMace result item is null or AIR!");
                return null;
            }

            // Validate all materials exist before creating recipe
            if (!validateMaterials()) {
                plugin.getLogger().severe("One or more materials for EarthMace recipe don't exist!");
                return null;
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

            // Set the pattern - ensure it's exactly 3 rows of 3 characters each
            recipe.shape(
                    "MSM",
                    "PHP",
                    "DBD"
            );

            // Set ingredients with error checking
            recipe.setIngredient('M', Material.MOSS_BLOCK);
            recipe.setIngredient('S', Material.SCULK_CATALYST);
            recipe.setIngredient('P', Material.PINK_WOOL);
            recipe.setIngredient('H', Material.HEAVY_CORE);
            recipe.setIngredient('B', Material.BREEZE_ROD);
            recipe.setIngredient('D', Material.DEEPSLATE_EMERALD_ORE);

            plugin.getLogger().info("Successfully created EarthMace recipe");
            return recipe;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating EarthMace recipe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean validateMaterials() {
        Material[] requiredMaterials = {
                Material.MOSS_BLOCK,
                Material.SCULK_CATALYST,
                Material.PINK_WOOL,
                Material.HEAVY_CORE,
                Material.BREEZE_ROD,
                Material.DEEPSLATE_EMERALD_ORE
        };

        for (Material material : requiredMaterials) {
            if (material == null) {
                plugin.getLogger().severe("Material is null in EarthMace recipe validation");
                return false;
            }
        }
        return true;
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
        try {
            ItemStack earthMace = maceManager.createEarthMace();
            if (earthMace != null && earthMace.getType() != Material.AIR) {
                player.getInventory().addItem(earthMace);
                player.sendMessage(Component.text("🌍 You crafted an Earth Mace!")
                        .color(NamedTextColor.GREEN));
            } else {
                plugin.getLogger().severe("Failed to create EarthMace for player: " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in EarthMace onCraft: " + e.getMessage());
            e.printStackTrace();
        }
    }
}