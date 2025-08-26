package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.managers.MaceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive crafting restriction listener that prevents:
 * 1. Normal vanilla maces from being crafted anywhere
 * 2. Multiple elemental maces of the same type from being crafted
 * 3. Bypassing restrictions through hoppers, droppers, etc.
 */
public class CraftingRestrictionListener implements Listener {

    private final MaceManager maceManager;
    private final JavaPlugin plugin;

    // Track how many of each mace type each player has crafted
    private final Map<UUID, Map<String, Integer>> playerMaceCounts = new HashMap<>();

    // Mace type constants
    private static final String AIR_MACE = "AIR_MACE";
    private static final String FIRE_MACE = "FIRE_MACE";
    private static final String WATER_MACE = "WATER_MACE";
    private static final String EARTH_MACE = "EARTH_MACE";

    private static final int MAX_MACES_PER_TYPE = 1;

    public CraftingRestrictionListener(MaceManager maceManager, JavaPlugin plugin) {
        this.maceManager = maceManager;
        this.plugin = plugin;
    }

    // Handle all player crafting (crafting table, inventory crafting)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();

        if (result.getType() == Material.MACE) {
            // Check if this is a vanilla mace (no custom name/lore)
            if (!result.hasItemMeta() ||
                    result.getItemMeta().displayName() == null ||
                    !result.getItemMeta().hasLore()) {

                // This is a vanilla mace recipe
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Normal maces are disabled! Craft elemental maces instead!")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Use /element to see available elemental maces!")
                        .color(NamedTextColor.YELLOW));
                return;
            }

            // Check if this is one of our custom maces and if player has reached limit
            String maceType = getMaceType(result);
            if (maceType != null) {
                int currentCount = getPlayerMaceCount(player.getUniqueId(), maceType);

                if (currentCount >= MAX_MACES_PER_TYPE) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ You can only craft " + MAX_MACES_PER_TYPE + " " +
                                    maceType.toLowerCase().replace("_", " ") + "!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Allow crafting and increment counter
                incrementPlayerMaceCount(player.getUniqueId(), maceType);
                player.sendMessage(Component.text("✅ " + maceType.toLowerCase().replace("_", " ") +
                                " crafted! (" + (currentCount + 1) + "/" + MAX_MACES_PER_TYPE + ")")
                        .color(NamedTextColor.GREEN));
            }
        }
    }

    // Handle preparation in crafting interfaces (prevents showing recipe)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        if (result != null && result.getType() == Material.MACE) {
            // Check if this is a vanilla mace
            if (!result.hasItemMeta() ||
                    result.getItemMeta().displayName() == null ||
                    !result.getItemMeta().hasLore()) {

                // Block vanilla mace recipe
                event.getInventory().setResult(null);
                return;
            }

            // For custom maces, check if any viewer has reached the limit
            String maceType = getMaceType(result);
            if (maceType != null) {
                for (org.bukkit.entity.HumanEntity viewer : event.getViewers()) {
                    if (viewer instanceof Player) {
                        Player player = (Player) viewer;
                        int currentCount = getPlayerMaceCount(player.getUniqueId(), maceType);

                        if (currentCount >= MAX_MACES_PER_TYPE) {
                            event.getInventory().setResult(null);
                            return;
                        }
                    }
                }
            }
        }
    }

    // Handle hopper transfers that might bypass crafting restrictions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();

        if (item.getType() == Material.MACE) {
            // Check if this is a vanilla mace being moved
            if (!item.hasItemMeta() ||
                    item.getItemMeta().displayName() == null ||
                    !item.getItemMeta().hasLore()) {

                // Block movement of vanilla maces
                event.setCancelled(true);
            }
        }
    }

    // Handle clicking in crafting result slots
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if clicking in result slot of crafting inventory
        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack currentItem = event.getCurrentItem();

            if (currentItem != null && currentItem.getType() == Material.MACE) {
                // Check if vanilla mace
                if (!currentItem.hasItemMeta() ||
                        currentItem.getItemMeta().displayName() == null ||
                        !currentItem.getItemMeta().hasLore()) {

                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ Normal maces are disabled!")
                            .color(NamedTextColor.RED));
                    return;
                }

                // Check custom mace limits
                String maceType = getMaceType(currentItem);
                if (maceType != null) {
                    int currentCount = getPlayerMaceCount(player.getUniqueId(), maceType);

                    if (currentCount >= MAX_MACES_PER_TYPE) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("❌ You can only have " + MAX_MACES_PER_TYPE +
                                        " " + maceType.toLowerCase().replace("_", " ") + "!")
                                .color(NamedTextColor.RED));
                    }
                }
            }
        }
    }

    /**
     * Determine the mace type from an ItemStack
     */
    private String getMaceType(ItemStack item) {
        if (maceManager.isAirMace(item)) {
            return AIR_MACE;
        } else if (maceManager.isFireMace(item)) {
            return FIRE_MACE;
        } else if (maceManager.isWaterMace(item)) {
            return WATER_MACE;
        } else if (maceManager.isEarthMace(item)) {
            return EARTH_MACE;
        }
        return null;
    }

    /**
     * Get the current count of a specific mace type for a player
     */
    private int getPlayerMaceCount(UUID playerId, String maceType) {
        return playerMaceCounts
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .getOrDefault(maceType, 0);
    }

    /**
     * Increment the count of a specific mace type for a player
     */
    private void incrementPlayerMaceCount(UUID playerId, String maceType) {
        Map<String, Integer> playerCounts = playerMaceCounts.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCounts.put(maceType, playerCounts.getOrDefault(maceType, 0) + 1);
    }

    /**
     * Reset mace counts for a player (useful for admin commands)
     */
    public void resetPlayerMaceCounts(UUID playerId) {
        playerMaceCounts.remove(playerId);
    }

    /**
     * Reset mace count for a specific type for a player
     */
    public void resetPlayerMaceCount(UUID playerId, String maceType) {
        Map<String, Integer> playerCounts = playerMaceCounts.get(playerId);
        if (playerCounts != null) {
            playerCounts.remove(maceType);
        }
    }

    /**
     * Get all mace counts for a player (for admin/debug purposes)
     */
    public Map<String, Integer> getPlayerMaceCounts(UUID playerId) {
        return new HashMap<>(playerMaceCounts.getOrDefault(playerId, new HashMap<>()));
    }
}