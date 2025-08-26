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
            // Reset own maces (players only)
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("❌ Console must specify a player to reset maces for!")
                        .color(NamedTextColor.RED));
                return true;
            }

            Player player = (Player) sender;
            resetPlayerMaces(player, player);
            return true;
        }

        if (args.length == 1) {
            // Reset another player's maces (admin only)
            if (!sender.hasPermission("element.admin")) {
                sender.sendMessage(Component.text("❌ You don't have permission to reset other players' maces!")
                        .color(NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("❌ Player '" + args[0] + "' not found or not online!")
                        .color(NamedTextColor.RED));
                return true;
            }

            resetPlayerMaces(sender, target);
            return true;
        }

        if (args.length == 2) {
            // Reset specific mace type for a player (admin only)
            if (!sender.hasPermission("element.admin")) {
                sender.sendMessage(Component.text("❌ You don't have permission to reset specific mace types!")
                        .color(NamedTextColor.RED));
                return true;
            }

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
        sender.sendMessage(Component.text("❌ Usage: /resetmaces [player] [mace_type]")
                .color(NamedTextColor.RED));
        return true;
    }

    private void resetPlayerMaces(CommandSender sender, Player target) {
        // Reset crafting counts
        craftingListener.resetAllPlayerMaceCounts(target.getUniqueId());

        // Send confirmation messages
        if (sender.equals(target)) {
            target.sendMessage(Component.text("✅ Your mace crafting limits have been reset!")
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("You can now craft one of each mace type again!")
                    .color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("✅ Reset mace crafting limits for " + target.getName())
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("🔄 Your mace crafting limits have been reset by " + sender.getName() + "!")
                    .color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("You can now craft one of each mace type again!")
                    .color(NamedTextColor.YELLOW));
        }

        // Log the action
        String logMessage = sender.equals(target) ?
                target.getName() + " reset their own mace crafting limits" :
                sender.getName() + " reset mace crafting limits for " + target.getName();
        elementManager.getPlugin().getLogger().info(logMessage);
    }

    private void resetSpecificMaceType(CommandSender sender, Player target, String maceType) {
        // Reset specific mace type count
        craftingListener.resetPlayerMaceCount(target.getUniqueId(), maceType);

        String maceDisplayName = getMaceDisplayName(maceType);

        // Send confirmation messages
        sender.sendMessage(Component.text("✅ Reset " + maceDisplayName + " crafting limit for " + target.getName())
                .color(NamedTextColor.GREEN));

        target.sendMessage(Component.text("🔄 Your " + maceDisplayName + " crafting limit has been reset by " + sender.getName() + "!")
                .color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("You can now craft the " + maceDisplayName + " again!")
                .color(NamedTextColor.YELLOW));

        // Log the action
        elementManager.getPlugin().getLogger().info(sender.getName() + " reset " + maceDisplayName + " crafting limit for " + target.getName());
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
}