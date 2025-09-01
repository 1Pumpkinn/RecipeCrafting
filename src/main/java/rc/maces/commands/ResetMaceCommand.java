package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.listeners.CraftingListener;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

public class ResetMaceCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final MaceManager maceManager;
    private final CraftingListener craftingListener;

    public ResetMaceCommand(ElementManager elementManager, MaceManager maceManager, CraftingListener craftingListener) {
        this.elementManager = elementManager;
        this.maceManager = maceManager;
        this.craftingListener = craftingListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // All reset operations require admin permission
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("❌ You don't have permission to reset maces!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        if (args.length == 1) {
            String arg = args[0].toLowerCase();

            // Handle "all" to reset all mace types
            if (arg.equals("all")) {
                resetAllMaces(sender);
                return true;
            }

            // Handle specific mace types
            String maceType = getMaceTypeFromArg(arg);
            if (maceType != null) {
                resetSpecificMaceType(sender, maceType);
                return true;
            }

            // Invalid argument
            sender.sendMessage(Component.text("❌ Invalid mace type! Use: air, fire, water, earth, or all")
                    .color(NamedTextColor.RED));
            showUsage(sender);
            return true;
        }

        // Too many arguments
        showUsage(sender);
        return true;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== MACE RESET COMMANDS ===")
                .color(NamedTextColor.GOLD));

        sender.sendMessage(Component.text("Usage:")
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("• /resetmaces all - Reset all mace types globally")
                .color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("• /resetmaces air - Reset Air Mace globally")
                .color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("• /resetmaces fire - Reset Fire Mace globally")
                .color(NamedTextColor.RED));
        sender.sendMessage(Component.text("• /resetmaces water - Reset Water Mace globally")
                .color(NamedTextColor.BLUE));
        sender.sendMessage(Component.text("• /resetmaces earth - Reset Earth Mace globally")
                .color(NamedTextColor.GREEN));

        sender.sendMessage(Component.text("This will make the mace available for crafting again!")
                .color(NamedTextColor.GRAY));
    }

    private void resetAllMaces(CommandSender sender) {
        craftingListener.resetAllGlobalMaceStatuses();

        // Announce to all players
        Component announcement = Component.text("🔄 ALL MACES HAVE BEEN RESET! All mace types are now available to craft again!")
                .color(NamedTextColor.GOLD);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(announcement);
        }

        sender.sendMessage(Component.text("✅ Reset all global mace crafting limits!")
                .color(NamedTextColor.GREEN));

        elementManager.getPlugin().getLogger().info(sender.getName() + " reset all global mace crafting limits");
    }

    private void resetSpecificMaceType(CommandSender sender, String maceType) {
        String oldCrafter = craftingListener.getGlobalMaceCrafter(maceType);
        boolean wasAlreadyCrafted = craftingListener.isGlobalMaceCrafted(maceType);

        craftingListener.resetGlobalMaceStatus(maceType);

        String maceDisplayName = getMaceDisplayName(maceType);
        NamedTextColor maceColor = getElementColor(maceType);

        if (wasAlreadyCrafted) {
            // Announce to all players
            Component announcement = Component.text("🔄 The " + maceDisplayName + " is now available to craft again!")
                    .color(maceColor);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(announcement);
            }

            sender.sendMessage(Component.text("✅ Reset global " + maceDisplayName + " status!")
                    .color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Previously crafted by: " + oldCrafter)
                    .color(NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("ℹ️ " + maceDisplayName + " was already available for crafting")
                    .color(NamedTextColor.YELLOW));
        }

        elementManager.getPlugin().getLogger().info(sender.getName() + " reset global " + maceDisplayName + " status");
    }

    private String getMaceTypeFromArg(String arg) {
        switch (arg.toLowerCase()) {
            case "air":
            case "airmace":
            case "wind":
                return "AIR";
            case "fire":
            case "firemace":
            case "flame":
                return "FIRE";
            case "water":
            case "watermace":
            case "aqua":
                return "WATER";
            case "earth":
            case "earthmace":
            case "ground":
                return "EARTH";
            default:
                return null;
        }
    }

    private String getMaceDisplayName(String maceType) {
        switch (maceType) {
            case "AIR": return "Air Mace";
            case "FIRE": return "Fire Mace";
            case "WATER": return "Water Mace";
            case "EARTH": return "Earth Mace";
            default: return "Unknown Mace";
        }
    }

    private NamedTextColor getElementColor(String maceType) {
        switch (maceType) {
            case "AIR": return NamedTextColor.WHITE;
            case "FIRE": return NamedTextColor.RED;
            case "WATER": return NamedTextColor.BLUE;
            case "EARTH": return NamedTextColor.GREEN;
            default: return NamedTextColor.GRAY;
        }
    }
}