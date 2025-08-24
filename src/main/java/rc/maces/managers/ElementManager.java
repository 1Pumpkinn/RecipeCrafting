package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ElementManager {

    private final JavaPlugin plugin;
    private final Map<UUID, String> playerElements;
    private final Random random;
    private final File dataFile;
    private final FileConfiguration dataConfig;

    // Element constants
    public static final String FIRE = "FIRE";
    public static final String WATER = "WATER";
    public static final String EARTH = "EARTH";
    public static final String AIR = "AIR";

    private static final String[] ELEMENTS = {FIRE, WATER, EARTH, AIR};

    public ElementManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerElements = new HashMap<>();
        this.random = new Random();

        // Initialize data file
        this.dataFile = new File(plugin.getDataFolder(), "elements.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Load existing data
        loadElementData();
    }

    /**
     * Loads element data from file
     */
    private void loadElementData() {
        try {
            if (dataFile.exists()) {
                for (String uuidString : dataConfig.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        String element = dataConfig.getString(uuidString);
                        if (element != null && isValidElement(element)) {
                            playerElements.put(uuid, element.toUpperCase());
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in elements.yml: " + uuidString);
                    }
                }
                plugin.getLogger().info("Loaded " + playerElements.size() + " player elements from file.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load element data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves element data to file
     */
    private void saveElementData() {
        try {
            // Clear existing data
            for (String key : dataConfig.getKeys(false)) {
                dataConfig.set(key, null);
            }

            // Save current data
            for (Map.Entry<UUID, String> entry : playerElements.entrySet()) {
                dataConfig.set(entry.getKey().toString(), entry.getValue());
            }

            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save element data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Assigns a random element to a player when they join (only if they don't have one)
     */
    public void assignRandomElement(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player already has an element
        if (playerElements.containsKey(playerId)) {
            String existingElement = playerElements.get(playerId);
            // Send welcome back message
            Component message = createWelcomeBackMessage(existingElement, player.getName());
            player.sendMessage(message);
            plugin.getLogger().info("Player " + player.getName() + " rejoined with existing element: " + existingElement);
            return;
        }

        // Assign new random element
        String element = ELEMENTS[random.nextInt(ELEMENTS.length)];
        playerElements.put(playerId, element);
        saveElementData(); // Save immediately

        // Send welcome message with element assignment
        Component message = createElementMessage(element, player.getName());
        player.sendMessage(message);

        plugin.getLogger().info("Assigned " + element + " element to new player " + player.getName());
    }

    /**
     * Gets the element assigned to a player
     */
    public String getPlayerElement(Player player) {
        return playerElements.get(player.getUniqueId());
    }

    /**
     * Checks if a player can craft a specific mace type
     */
    public boolean canCraftMace(Player player, String maceType) {
        String playerElement = getPlayerElement(player);
        return playerElement != null && playerElement.equals(maceType.toUpperCase());
    }

    /**
     * Gets the display name for an element
     */
    public String getElementDisplayName(String element) {
        switch (element) {
            case FIRE: return "🔥 Fire";
            case WATER: return "🌊 Water";
            case EARTH: return "🌍 Earth";
            case AIR: return "💨 Air";
            default: return "Unknown";
        }
    }

    /**
     * Gets the color for an element
     */
    public NamedTextColor getElementColor(String element) {
        switch (element) {
            case FIRE: return NamedTextColor.RED;
            case WATER: return NamedTextColor.BLUE;
            case EARTH: return NamedTextColor.GREEN;
            case AIR: return NamedTextColor.WHITE;
            default: return NamedTextColor.GRAY;
        }
    }

    /**
     * Creates a formatted message for existing players
     */
    private Component createWelcomeBackMessage(String element, String playerName) {
        return Component.text("🌟 WELCOME BACK! 🌟")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("Player: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(playerName)
                        .color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Your Element: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(element))
                        .color(getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("You can only craft the " + element.toLowerCase() + " mace!")
                        .color(NamedTextColor.GRAY));
    }

    /**
     * Creates a formatted message for element assignment
     */
    private Component createElementMessage(String element, String playerName) {
        return Component.text("🌟 ELEMENT ASSIGNMENT 🌟")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("Player: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(playerName)
                        .color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Your Element: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(element))
                        .color(getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("You can only craft the " + element.toLowerCase() + " mace!")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Some passive abilities work even without holding your mace!")
                        .color(NamedTextColor.GRAY));
    }

    /**
     * Removes a player's element data when they leave (NO LONGER USED)
     * Keeping for compatibility but not removing data
     */
    @Deprecated
    public void removePlayerElement(Player player) {
        // Do nothing - we want to keep the data persistent
        // This method is kept for compatibility but doesn't remove data anymore
    }

    /**
     * Saves all element data to file (called on plugin disable)
     */
    public void saveAllData() {
        saveElementData();
        plugin.getLogger().info("Saved all element data to file.");
    }

    /**
     * Gets all player elements (for debugging)
     */
    public Map<UUID, String> getAllPlayerElements() {
        return new HashMap<>(playerElements);
    }

    /**
     * Manually set a player's element (admin command)
     */
    public void setPlayerElement(Player player, String element) {
        if (isValidElement(element)) {
            playerElements.put(player.getUniqueId(), element.toUpperCase());
            saveElementData(); // Save immediately
            Component message = createElementMessage(element.toUpperCase(), player.getName());
            player.sendMessage(message);
        }
    }

    /**
     * Checks if an element is valid
     */
    public boolean isValidElement(String element) {
        for (String validElement : ELEMENTS) {
            if (validElement.equalsIgnoreCase(element)) {
                return true;
            }
        }
        return false;
    }
}