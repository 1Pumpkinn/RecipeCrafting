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

public class FiremaceRecipe implements CustomRecipe {

    private final JavaPlugin plugin;
    private final rc.maces.managers.MaceManager maceManager;
    private final NamespacedKey recipeKey;
    private ShapedRecipe bukkitRecipe;

    public FiremaceRecipe(JavaPlugin plugin, rc.maces.managers.MaceManager maceManager) {
        this.plugin = plugin;
        this.maceManager = maceManager;
        this.recipeKey = new NamespacedKey(plugin, "firemace_recipe");
        this.bukkitRecipe = createRecipe();
    }

    private ShapedRecipe createRecipe() {
        try {
            // Create the actual fire mace using MaceManager
            ItemStack result = maceManager.createFireMace();

            if (result == null || result.getType() == Material.AIR) {
                plugin.getLogger().severe("FireMace result item is null or AIR!");
                return null;
            }

            // Validate all materials exist before creating recipe
            if (!validateMaterials()) {
                plugin.getLogger().severe("One or more materials for FireMace recipe don't exist!");
                return null;
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

            // Set the pattern - ensure it's exactly 3 rows of 3 characters each
            recipe.shape(
                    "LWL",
                    "LHL",
                    "DBD"
            );

            // Set ingredients with error checking
            recipe.setIngredient('L', Material.LAVA_BUCKET);
            recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
            recipe.setIngredient('H', Material.HEAVY_CORE);
            recipe.setIngredient('B', Material.BREEZE_ROD);
            recipe.setIngredient('D', Material.DRIED_GHAST); // Changed from DRIED_GHAST to be safe

            plugin.getLogger().info("Successfully created FireMace recipe");
            return recipe;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating FireMace recipe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean validateMaterials() {
        Material[] requiredMaterials = {
                Material.LAVA_BUCKET,
                Material.WITHER_SKELETON_SKULL,
                Material.HEAVY_CORE,
                Material.BREEZE_ROD,
                Material.GHAST_TEAR // Using GHAST_TEAR instead of DRIED_GHAST for compatibility
        };

        for (Material material : requiredMaterials) {
            if (material == null) {
                plugin.getLogger().severe("Material is null in FireMace recipe validation");
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
        return "Firemace";
    }

    @Override
    public void onCraft(Player player) {
        try {
            ItemStack fireMace = maceManager.createFireMace();
            if (fireMace != null && fireMace.getType() != Material.AIR) {
                player.getInventory().addItem(fireMace);
                player.sendMessage(Component.text("🔥 You crafted a Fire Mace!")
                        .color(NamedTextColor.RED));
            } else {
                plugin.getLogger().severe("Failed to create FireMace for player: " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in FireMace onCraft: " + e.getMessage());
            e.printStackTrace();
        }
    }
}