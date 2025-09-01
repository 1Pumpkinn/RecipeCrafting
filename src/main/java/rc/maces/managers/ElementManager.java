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
            plugin.getLogger().severe("Error while loading data");
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
            // FIXED: Send simple welcome back message without mace abilities spam
            player.sendMessage(Component.text("🌟 Welcome back! Your element is ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text(getElementDisplayName(existingElement))
                            .color(getElementColor(existingElement))
                            .decoration(TextDecoration.BOLD, true)));
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
     * Changes a player's element to match a picked up mace
     */
    public void switchElementToMace(Player player, String maceElement) {
        UUID playerId = player.getUniqueId();
        String currentElement = playerElements.get(playerId);

        if (maceElement.equals(currentElement)) {
            return; // Already has this element
        }

        // Switch element immediately
        playerElements.put(playerId, maceElement);
        saveElementData();

        // FIXED: Send simple notification without mace abilities spam
        player.sendMessage(Component.text("⚡ Your element changed to ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(getElementDisplayName(maceElement))
                        .color(getElementColor(maceElement))
                        .decoration(TextDecoration.BOLD, true)));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        plugin.getLogger().info("Switched " + player.getName() + "'s element from " +
                (currentElement != null ? currentElement : "NONE") + " to " + maceElement);
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

    public void clearPlayerElement(Player player) {
        playerElements.remove(player.getUniqueId());
        saveElementData();

        player.sendMessage(Component.text("🔄 Your element has been cleared!")
                .color(NamedTextColor.GRAY));
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
            // FIXED: Send simple assignment message without mace abilities spam
            Component finalMessage = rerolling ?
                    Component.text("🎲 ELEMENT REROLLED! ")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true)
                            .append(Component.text("(" + getElementDisplayName(prevElement) + " → " + getElementDisplayName(resultElement) + ")")
                                    .color(getElementColor(resultElement))
                                    .decoration(TextDecoration.BOLD, true)) :
                    Component.text("⚡ ELEMENT ASSIGNED! ")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true)
                            .append(Component.text(getElementDisplayName(resultElement))
                                    .color(getElementColor(resultElement))
                                    .decoration(TextDecoration.BOLD, true));

            target.sendMessage(finalMessage);
            target.sendMessage(Component.text("Use /myelement to see your abilities and passives!")
                    .color(NamedTextColor.AQUA));

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
            case FIRE:
                return "🔥 Fire";
            case WATER:
                return "🌊 Water";
            case EARTH:
                return "🌍 Earth";
            case AIR:
                return "💨 Air";
            default:
                return "Unknown";
        }
    }

    public NamedTextColor getElementColor(String element) {
        switch (element) {
            case FIRE:
                return NamedTextColor.RED;
            case WATER:
                return NamedTextColor.BLUE;
            case EARTH:
                return NamedTextColor.GREEN;
            case AIR:
                return NamedTextColor.WHITE;
            default:
                return NamedTextColor.GRAY;
        }
    }

    /**
     * Gets detailed element information with all abilities
     */
    public Component getDetailedElementInfo(String element) {
        Component baseInfo = Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY)
                .appendNewline()
                .append(Component.text("Element: ").color(NamedTextColor.YELLOW))
                .append(Component.text(getElementDisplayName(element))
                        .color(getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline();

        Component elementSpecific;
        switch (element) {
            case FIRE:
                elementSpecific = Component.text("🔥 ABILITIES:").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                        .appendNewline()
                        .append(Component.text("• Right-click: Obsidian Creation (30s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• F key: Meteors (25s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("🔥 PASSIVES:").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
                        .appendNewline()
                        .append(Component.text("• Fire Resistance").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• +1 Attack Damage when on fire").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Ignite enemies on hit").color(NamedTextColor.GRAY));
                break;
            case WATER:
                elementSpecific = Component.text("🌊 ABILITIES:").color(NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true)
                        .appendNewline()
                        .append(Component.text("• Right-click: Water Heal +2❤ (10s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• F key: Water Geyser (30s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("🌊 PASSIVES:").color(NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true))
                        .appendNewline()
                        .append(Component.text("• 1% chance Mining Fatigue on hit").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Dolphins Grace in water").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Conduit Power").color(NamedTextColor.GRAY));
                break;
            case EARTH:
                elementSpecific = Component.text("🌍 ABILITIES:").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)
                        .appendNewline()
                        .append(Component.text("• Right-click: Buddy Up (15s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• F key: Vine Trap (25s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("🌍 PASSIVES:").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
                        .appendNewline()
                        .append(Component.text("• Hero of the Village").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Haste 3 (Haste 5 with mace)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• All food = golden apples (with mace)").color(NamedTextColor.GRAY));
                break;
            case AIR:
                elementSpecific = Component.text("💨 ABILITIES:").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)
                        .appendNewline()
                        .append(Component.text("• Right-click: Wind Shot (5s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• F key: Wind Struck (25s cooldown)").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("💨 PASSIVES:").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                        .appendNewline()
                        .append(Component.text("• Speed 1").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• No fall damage").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Wind charges pull entities").color(NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("• Hit gives slow falling").color(NamedTextColor.GRAY));
                break;
            default:
                elementSpecific = Component.text("Unknown element").color(NamedTextColor.GRAY);
                break;
        }

        return baseInfo.append(elementSpecific)
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY));
    }

    /**
     * Gets a list of all elements with their display names
     */
    public Component getElementList() {
        return Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY)
                .appendNewline()
                .append(Component.text("Available Elements:").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("🔥 Fire - Fire Resistance, +1 Attack when burning").color(NamedTextColor.RED))
                .appendNewline()
                .append(Component.text("🌊 Water - 1% Mining Fatigue, Dolphins Grace, Conduit Power").color(NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("🌍 Earth - Hero of Village, Haste 3, Golden apple food").color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("💨 Air - Speed 1, No fall damage, Wind charge pull").color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("═══════════════════════════").color(NamedTextColor.DARK_GRAY))
                .appendNewline()
                .append(Component.text("Use /element <element> to see detailed info!").color(NamedTextColor.YELLOW));
    }

    public void saveAllData() {
        saveElementData();
        plugin.getLogger().info("Saved all element data to file.");
    }

    public Map<UUID, String> getAllPlayerElements() {
        return new HashMap<>(playerElements);
    }

    public void setPlayerElement(Player player, String element) {
        playerElements.put(player.getUniqueId(), element); {
            playerElements.put(player.getUniqueId(), element.toUpperCase());
            saveElementData();
            // FIXED: Simple message without mace abilities spam
            player.sendMessage(Component.text("⚡ Your element has been set to ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text(getElementDisplayName(element.toUpperCase()))
                            .color(getElementColor(element.toUpperCase()))
                            .decoration(TextDecoration.BOLD, true)));
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

    @Deprecated
    public void removePlayerElement(Player player) {
        // kept for compatibility
    }
}