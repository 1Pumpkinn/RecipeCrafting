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

public class WatermaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private ShapedRecipe bukkitRecipe;

    public WatermaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "watermace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        try {
            // Create the actual water mace using MaceManager
            ItemStack result = maceManager.createWaterMace();

            if (result == null || result.getType() == Material.AIR) {
                plugin.getLogger().severe("WaterMace result item is null or AIR!");
                return null;
            }

            // Validate all materials exist before creating recipe
            if (!validateMaterials()) {
                plugin.getLogger().severe("One or more materials for WaterMace recipe don't exist!");
                return null;
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

            // Set the pattern - ensure it's exactly 3 rows of 3 characters each
            recipe.shape(
                    "WCW",
                    "THF",
                    "QBQ"
            );

            // Set ingredients with error checking
            recipe.setIngredient('C', Material.CONDUIT);
            recipe.setIngredient('W', Material.TROPICAL_FISH);
            recipe.setIngredient('T', Material.TUBE_CORAL);
            recipe.setIngredient('F', Material.FIRE_CORAL);
            recipe.setIngredient('H', Material.HEAVY_CORE);
            recipe.setIngredient('Q', Material.TRIDENT);
            recipe.setIngredient('B', Material.BREEZE_ROD);

            plugin.getLogger().info("Successfully created WaterMace recipe");
            return recipe;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating WaterMace recipe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean validateMaterials() {
        Material[] requiredMaterials = {
                Material.CONDUIT,
                Material.TROPICAL_FISH,
                Material.TUBE_CORAL,
                Material.FIRE_CORAL,
                Material.HEAVY_CORE,
                Material.TRIDENT,
                Material.BREEZE_ROD
        };

        for (Material material : requiredMaterials) {
            if (material == null) {
                plugin.getLogger().severe("Material is null in WaterMace recipe validation");
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
        return "Watermace";
    }

    @Override
    public void onCraft(Player player) {
        try {
            ItemStack waterMace = maceManager.createWaterMace();
            if (waterMace != null && waterMace.getType() != Material.AIR) {
                player.getInventory().addItem(waterMace);
                player.sendMessage(Component.text("🌊 You crafted a Water Mace!")
                        .color(NamedTextColor.BLUE));
            } else {
                plugin.getLogger().severe("Failed to create WaterMace for player: " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in WaterMace onCraft: " + e.getMessage());
            e.printStackTrace();
        }
    }
}