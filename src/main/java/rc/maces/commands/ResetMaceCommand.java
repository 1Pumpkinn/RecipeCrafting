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
        if (args.length == 0) {
            // Show help/usage for regular players
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("❌ Console must specify arguments!")
                        .color(NamedTextColor.RED));
                showUsage(sender);
                return true;
            }

            showUsage(sender);
            return true;
        }

        // All reset operations require admin permission
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("❌ You don't have permission to reset maces!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            String arg = args[0].toLowerCase();

            // NEW: Global reset commands
            if (arg.equals("global")) {
                resetAllGlobalMaces(sender);
                return true;
            } else if (arg.equals("globalair") || arg.equals("global-air")) {
                resetGlobalMaceType(sender, "AIR");
                return true;
            } else if (arg.equals("globalfire") || arg.equals("global-fire")) {
                resetGlobalMaceType(sender, "FIRE");
                return true;
            } else if (arg.equals("globalwater") || arg.equals("global-water")) {
                resetGlobalMaceType(sender, "WATER");
                return true;
            } else if (arg.equals("globalearth") || arg.equals("global-earth")) {
                resetGlobalMaceType(sender, "EARTH");
                return true;
            } else {
                // Try to reset a specific player's personal counts
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(Component.text("❌ Player '" + args[0] + "' not found or not online!")
                            .color(NamedTextColor.RED));
                    return true;
                }

                resetPlayerMaces(sender, target);
                return true;
            }
        }

        if (args.length == 2) {
            // Reset specific mace type for a player (personal counts only)
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("❌ Player '" + args[0] + "' not found or not online!")
                        .color(NamedTextColor.RED));
                return true;
            }

            String maceType = args[1].toUpperCase();
            if (!isValidMaceType(maceType)) {
                sender.sendMessage(Component.text("❌ Invalid mace type! Use: air, fire, water, or earth")
                        .color(NamedTextColor.RED));
                return true;
            }

            resetSpecificMaceType(sender, target, maceType);
            return true;
        }

        // Too many arguments
        showUsage(sender);
        return true;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== MACE RESET COMMANDS ===")
                .color(NamedTextColor.GOLD));

        if (sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("Global Resets (Server-wide):")
                    .color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("• /resetmaces global - Reset all global mace crafting")
                    .color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("• /resetmaces globalair - Reset only Air Mace globally")
                    .color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("• /resetmaces globalfire - Reset only Fire Mace globally")
                    .color(NamedTextColor.RED));
            sender.sendMessage(Component.text("• /resetmaces globalwater - Reset only Water Mace globally")
                    .color(NamedTextColor.BLUE));
            sender.sendMessage(Component.text("• /resetmaces globalearth - Reset only Earth Mace globally")
                    .color(NamedTextColor.GREEN));

            sender.sendMessage(Component.text("Personal Resets:")
                    .color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("• /resetmaces <player> - Reset player's personal counts")
                    .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("• /resetmaces <player> <mace_type> - Reset specific type")
                    .color(NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("❌ You need admin permissions to reset maces!")
                    .color(NamedTextColor.RED));
        }
    }

    private void resetAllGlobalMaces(CommandSender sender) {
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

    private void resetGlobalMaceType(CommandSender sender, String maceType) {
        String oldCrafter = craftingListener.getGlobalMaceCrafter(maceType);
        boolean wasAlreadyCrafted = craftingListener.isGlobalMaceCrafted(maceType);

        craftingListener.resetGlobalMaceStatus(maceType);

        String maceDisplayName = getMaceDisplayName(maceType);

        if (wasAlreadyCrafted) {
            // Announce to all players
            Component announcement = Component.text("🔄 The " + maceDisplayName + " is now available to craft again!")
                    .color(getElementColor(maceType));

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

    private void resetPlayerMaces(CommandSender sender, Player target) {
        // Reset personal crafting counts (display only now)
        craftingListener.resetAllPlayerMaceCounts(target.getUniqueId());

        // Send confirmation messages
        if (sender.equals(target)) {
            target.sendMessage(Component.text("✅ Your personal mace records have been cleared!")
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("Note: This only affects your personal display counts")
                    .color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("✅ Reset personal mace records for " + target.getName())
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("🔄 Your personal mace records have been cleared by " + sender.getName() + "!")
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("Note: This only affects your personal display counts")
                    .color(NamedTextColor.YELLOW));
        }

        // Log the action
        String logMessage = sender.equals(target) ?
                target.getName() + " reset their own personal mace records" :
                sender.getName() + " reset personal mace records for " + target.getName();
        elementManager.getPlugin().getLogger().info(logMessage);
    }

    private void resetSpecificMaceType(CommandSender sender, Player target, String maceType) {
        // Reset specific personal mace type count
        craftingListener.resetPlayerMaceCount(target.getUniqueId(), maceType);

        String maceDisplayName = getMaceDisplayName(maceType);

        // Send confirmation messages
        sender.sendMessage(Component.text("✅ Reset " + maceDisplayName + " personal record for " + target.getName())
                .color(NamedTextColor.GREEN));

        target.sendMessage(Component.text("🔄 Your " + maceDisplayName + " personal record has been cleared by " + sender.getName() + "!")
                .color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("Note: This only affects your personal display counts")
                .color(NamedTextColor.YELLOW));

        // Log the action
        elementManager.getPlugin().getLogger().info(sender.getName() + " reset " + maceDisplayName + " personal record for " + target.getName());
    }

    private boolean isValidMaceType(String maceType) {
        return maceType.equals("AIR") || maceType.equals("FIRE") ||
                maceType.equals("WATER") || maceType.equals("EARTH");
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