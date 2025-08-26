package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.listeners.CraftingListener;
import rc.maces.managers.ElementManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to reset mace crafting counts and player elements
 * Usage: /resetmace <element> [player]
 * Elements: air, fire, water, earth, all
 */
public class ResetMaceCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final CraftingListener craftingListener;
    private final ElementManager elementManager;

    public ResetMaceCommand(JavaPlugin plugin, CraftingListener craftingListener, ElementManager elementManager) {
        this.plugin = plugin;
        this.craftingListener = craftingListener;
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("maces.admin.reset")) {
            sender.sendMessage(Component.text("❌ You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(Component.text("Usage: /resetmace <element> [player]")
                    .color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Elements: air, fire, water, earth, all")
                    .color(NamedTextColor.GRAY));
            return true;
        }

        String element = args[0].toLowerCase();
        Player targetPlayer = null;

        // Determine target player
        if (args.length == 2) {
            // Target specified player
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text("❌ Player '" + args[1] + "' not found!")
                        .color(NamedTextColor.RED));
                return true;
            }
        } else {
            // Target sender (must be a player)
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("❌ Console must specify a player: /resetmace <element> <player>")
                        .color(NamedTextColor.RED));
                return true;
            }
            targetPlayer = (Player) sender;
        }

        // Validate element
        if (!isValidElement(element)) {
            sender.sendMessage(Component.text("❌ Invalid element! Valid elements: air, fire, water, earth, all")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Perform reset
        if (element.equals("all")) {
            resetAllMaces(sender, targetPlayer);
        } else {
            resetSpecificMace(sender, targetPlayer, element.toUpperCase());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Element completions
            List<String> elements = Arrays.asList("air", "fire", "water", "earth", "all");
            String input = args[0].toLowerCase();
            for (String element : elements) {
                if (element.startsWith(input)) {
                    completions.add(element);
                }
            }
        } else if (args.length == 2) {
            // Player name completions
            String input = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    /**
     * Check if the element string is valid
     */
    private boolean isValidElement(String element) {
        return element.equals("air") || element.equals("fire") ||
                element.equals("water") || element.equals("earth") ||
                element.equals("all");
    }

    /**
     * Reset all maces for a player
     */
    private void resetAllMaces(CommandSender sender, Player targetPlayer) {
        // Reset all mace counts
        craftingListener.resetAllPlayerMaceCounts(targetPlayer.getUniqueId());

        // Reset player element
        elementManager.clearPlayerElement(targetPlayer);

        // Send messages
        sender.sendMessage(Component.text("✅ Reset all maces and element for " + targetPlayer.getName())
                .color(NamedTextColor.GREEN));

        if (!sender.equals(targetPlayer)) {
            targetPlayer.sendMessage(Component.text("🔄 Your maces and element have been reset by an admin!")
                    .color(NamedTextColor.YELLOW));
        } else {
            targetPlayer.sendMessage(Component.text("🔄 All your maces and element have been reset!")
                    .color(NamedTextColor.GREEN));
        }
    }

    /**
     * Reset a specific mace type for a player
     */
    private void resetSpecificMace(CommandSender sender, Player targetPlayer, String maceType) {
        // Reset specific mace count
        craftingListener.resetPlayerMaceCount(targetPlayer.getUniqueId(), maceType);

        // Get element display name and color
        String displayName = getElementDisplayName(maceType);
        NamedTextColor elementColor = getElementColor(maceType);

        // Send messages
        sender.sendMessage(Component.text("✅ Reset " + displayName.toLowerCase() + " mace for " + targetPlayer.getName())
                .color(NamedTextColor.GREEN));

        if (!sender.equals(targetPlayer)) {
            targetPlayer.sendMessage(Component.text("🔄 Your " + displayName.toLowerCase() + " mace has been reset by an admin!")
                    .color(NamedTextColor.YELLOW));
        } else {
            targetPlayer.sendMessage(Component.text("🔄 Your " + displayName.toLowerCase() + " mace has been reset!")
                    .color(elementColor));
        }
    }

    /**
     * Get display name for element
     */
    private String getElementDisplayName(String element) {
        switch (element) {
            case "AIR": return "Air";
            case "FIRE": return "Fire";
            case "WATER": return "Water";
            case "EARTH": return "Earth";
            default: return "Unknown";
        }
    }

    /**
     * Get color for element
     */
    private NamedTextColor getElementColor(String element) {
        switch (element) {
            case "AIR": return NamedTextColor.WHITE;
            case "FIRE": return NamedTextColor.RED;
            case "WATER": return NamedTextColor.BLUE;
            case "EARTH": return NamedTextColor.GREEN;
            default: return NamedTextColor.GRAY;
        }
    }
}