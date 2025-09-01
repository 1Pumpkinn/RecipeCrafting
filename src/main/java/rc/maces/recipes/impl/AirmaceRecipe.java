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

public class AirmaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private ShapedRecipe bukkitRecipe;

    public AirmaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "airmace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        try {
            // Create the actual air mace using MaceManager
            ItemStack result = maceManager.createAirMace();

            if (result == null || result.getType() == Material.AIR) {
                plugin.getLogger().severe("AirMace result item is null or AIR!");
                return null;
            }

            // Validate all materials exist before creating recipe
            if (!validateMaterials()) {
                plugin.getLogger().severe("One or more materials for AirMace recipe don't exist!");
                return null;
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

            // Set the pattern - ensure it's exactly 3 rows of 3 characters each
            recipe.shape(
                    "GNG",
                    "PHP",
                    "WBW"
            );

            // Set ingredients with error checking
            recipe.setIngredient('G', Material.GHAST_TEAR);
            recipe.setIngredient('N', Material.NETHER_STAR);
            recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
            recipe.setIngredient('H', Material.HEAVY_CORE);
            recipe.setIngredient('W', Material.WIND_CHARGE);
            recipe.setIngredient('B', Material.BREEZE_ROD);

            plugin.getLogger().info("Successfully created AirMace recipe");
            return recipe;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating AirMace recipe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean validateMaterials() {
        Material[] requiredMaterials = {
                Material.GHAST_TEAR,
                Material.NETHER_STAR,
                Material.PHANTOM_MEMBRANE,
                Material.HEAVY_CORE,
                Material.WIND_CHARGE,
                Material.BREEZE_ROD
        };

        for (Material material : requiredMaterials) {
            if (material == null) {
                plugin.getLogger().severe("Material is null in AirMace recipe validation");
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
        return "Airmace";
    }

    @Override
    public void onCraft(Player player) {
        try {
            ItemStack airMace = maceManager.createAirMace();
            if (airMace != null && airMace.getType() != Material.AIR) {
                player.getInventory().addItem(airMace);
                player.sendMessage(Component.text("💨 You crafted an Air Mace!")
                        .color(NamedTextColor.WHITE));
            } else {
                plugin.getLogger().severe("Failed to create AirMace for player: " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in AirMace onCraft: " + e.getMessage());
            e.printStackTrace();
        }
    }
}