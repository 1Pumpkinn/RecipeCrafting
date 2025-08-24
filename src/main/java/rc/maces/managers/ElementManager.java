package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
            plugin.getLogger().severe("Error while saving data");
            plugin.getLogger().severe(e.getMessage());
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
     * Assigns a random element to a player with animation when they join
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

        // Show role rolling animation for new players
        showElementRollingAnimation(player, false);
    }

    /**
     * Rerolls a player's element (forces new assignment even if they have one)
     */
    public void rerollPlayerElement(Player player) {
        UUID playerId = player.getUniqueId();
        String currentElement = playerElements.get(playerId);

        if (currentElement != null) {
            plugin.getLogger().info("Rerolling element for " + player.getName() + " (was: " + currentElement + ")");
        }

        // Show reroll animation
        showElementRollingAnimation(player, true);
    }

    /**
     * Shows an animated element rolling sequence
     */
    private void showElementRollingAnimation(Player player, boolean isReroll) {
        final UUID playerId = player.getUniqueId();
        final String oldElement = playerElements.get(playerId);
        final Player target = player;
        final boolean rerolling = isReroll;

        Component startMessage = rerolling ?
                Component.text("🎲 REROLLING YOUR ELEMENT... 🎲")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true) :
                Component.text("🎲 ROLLING FOR YOUR ELEMENT... 🎲")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true);

        target.sendMessage(startMessage);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

        new BukkitRunnable() {
            int tickCount = 0;
            final int totalTicks = 60;
            String currentElement = ELEMENTS[0];

            @Override
            public void run() {
                tickCount++;

                // Change element display every few ticks, slowing down
                int changeInterval = Math.max(2, tickCount / 5);

                if (tickCount % changeInterval == 0) {
                    if (rerolling && oldElement != null) {
                        do {
                            currentElement = ELEMENTS[random.nextInt(ELEMENTS.length)];
                        } while (currentElement.equals(oldElement) && random.nextDouble() < 0.7);
                    } else {
                        currentElement = ELEMENTS[random.nextInt(ELEMENTS.length)];
                    }

                    Component rollingMessage = Component.text("🎲 Rolling... ")
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(getElementDisplayName(currentElement))
                                    .color(getElementColor(currentElement))
                                    .decoration(TextDecoration.BOLD, true));

                    target.sendActionBar(rollingMessage);
                    target.getWorld().playSound(target.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                            1.0f + (tickCount * 0.01f));
                }

                if (tickCount >= totalTicks) {
                    cancel();
                    finalizeElementAssignment(target, rerolling, oldElement);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Finalizes the element assignment after rolling animation
     */
    private void finalizeElementAssignment(Player player, boolean isReroll, String oldElement) {
        final UUID playerId = player.getUniqueId();

        // Assign final random element
        String finalElement;
        if (isReroll && oldElement != null) {
            do {
                finalElement = ELEMENTS[random.nextInt(ELEMENTS.length)];
            } while (finalElement.equals(oldElement));
        } else {
            finalElement = ELEMENTS[random.nextInt(ELEMENTS.length)];
        }

        playerElements.put(playerId, finalElement);
        saveElementData();

        player.sendActionBar(Component.empty());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);

        final Player target = player;
        final boolean rerolling = isReroll;
        final String prevElement = oldElement;
        final String resultElement = finalElement;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Component finalMessage = rerolling ?
                    createElementRerollMessage(resultElement, prevElement, target.getName()) :
                    createElementAssignmentMessage(resultElement, target.getName());
            target.sendMessage(finalMessage);

            Component title = rerolling ?
                    Component.text("ELEMENT REROLLED!")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true) :
                    Component.text("ELEMENT ASSIGNED!")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true);
            Component subtitle = Component.text(getElementDisplayName(resultElement))
                    .color(getElementColor(resultElement))
                    .decoration(TextDecoration.BOLD, true);

            target.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));

            String logMessage = rerolling ?
                    "Rerolled " + target.getName() + "'s element from " + prevElement + " to " + resultElement :
                    "Assigned " + resultElement + " element to new player " + target.getName();
            plugin.getLogger().info(logMessage);
        }, 10L);
    }

    // ------------------ utility methods ------------------

    public String getPlayerElement(Player player) {
        return playerElements.get(player.getUniqueId());
    }

    public boolean canCraftMace(Player player, String maceType) {
        String playerElement = getPlayerElement(player);
        return playerElement != null && playerElement.equals(maceType.toUpperCase());
    }

    public String getElementDisplayName(String element) {
        switch (element) {
            case FIRE: return "🔥 Fire";
            case WATER: return "🌊 Water";
            case EARTH: return "🌍 Earth";
            case AIR: return "💨 Air";
            default: return "Unknown";
        }
    }

    public NamedTextColor getElementColor(String element) {
        switch (element) {
            case FIRE: return NamedTextColor.RED;
            case WATER: return NamedTextColor.BLUE;
            case EARTH: return NamedTextColor.GREEN;
            case AIR: return NamedTextColor.WHITE;
            default: return NamedTextColor.GRAY;
        }
    }

    private Component createWelcomeBackMessage(String element, String playerName) {
        return Component.text("🌟 WELCOME BACK! 🌟")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("Player: ").color(NamedTextColor.YELLOW))
                .append(Component.text(playerName).color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Your Element: ").color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(element))
                        .color(getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("You can only craft the " + element.toLowerCase() + " mace!")
                        .color(NamedTextColor.GRAY));
    }

    private Component createElementAssignmentMessage(String element, String playerName) {
        return Component.text("⚡ ELEMENT ASSIGNMENT COMPLETE! ⚡")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("Player: ").color(NamedTextColor.YELLOW))
                .append(Component.text(playerName).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("Your Element: ").color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(element))
                        .color(getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("You can only craft the " + element.toLowerCase() + " mace!")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Some passive abilities work even without holding your mace!")
                        .color(NamedTextColor.DARK_AQUA))
                .appendNewline()
                .append(Component.text("Welcome to the elemental world!").color(NamedTextColor.GREEN));
    }

    private Component createElementRerollMessage(String newElement, String oldElement, String playerName) {
        return Component.text("🎲 ELEMENT REROLL COMPLETE! 🎲")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("Player: ").color(NamedTextColor.YELLOW))
                .append(Component.text(playerName).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("Previous Element: ").color(NamedTextColor.GRAY))
                .append(Component.text(getElementDisplayName(oldElement)).color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("NEW Element: ").color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(newElement))
                        .color(getElementColor(newElement))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("You can now craft the " + newElement.toLowerCase() + " mace!")
                        .color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("Your element has been changed!").color(NamedTextColor.AQUA));
    }

    @Deprecated
    public void removePlayerElement(Player player) {
        // kept for compatibility
    }

    public void saveAllData() {
        saveElementData();
        plugin.getLogger().info("Saved all element data to file.");
    }

    public Map<UUID, String> getAllPlayerElements() {
        return new HashMap<>(playerElements);
    }

    public void setPlayerElement(Player player, String element) {
        if (isValidElement(element)) {
            playerElements.put(player.getUniqueId(), element.toUpperCase());
            saveElementData();
            Component message = createElementAssignmentMessage(element.toUpperCase(), player.getName());
            player.sendMessage(message);
        }
    }

    public boolean isValidElement(String element) {
        for (String validElement : ELEMENTS) {
            if (validElement.equalsIgnoreCase(element)) {
                return true;
            }
        }
        return false;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
