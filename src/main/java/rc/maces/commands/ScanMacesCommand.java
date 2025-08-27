package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import rc.maces.listeners.CraftingListener;

public class ScanMacesCommand implements CommandExecutor {

    private final CraftingListener craftingListener;

    public ScanMacesCommand(CraftingListener craftingListener) {
        this.craftingListener = craftingListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission (you can customize this)
        if (!sender.hasPermission("maces.admin.scan") && !sender.isOp()) {
            sender.sendMessage(Component.text("❌ You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Check for arguments
        boolean includeOffline = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            includeOffline = true;
        }

        if (includeOffline) {
            sender.sendMessage(Component.text("🔍 Scanning ALL players (online and offline) for existing maces...")
                    .color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("⚠️  This may take a while for servers with many players!")
                    .color(NamedTextColor.GOLD));

            // Trigger the full scan
            craftingListener.scanAllPlayersIncludingOfflineCommand();

            sender.sendMessage(Component.text("✅ Complete mace scanning initiated! Check console for results.")
                    .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("🔍 Scanning online players for existing maces...")
                    .color(NamedTextColor.YELLOW));

            // Trigger the scan
            craftingListener.scanAllPlayersCommand();

            sender.sendMessage(Component.text("✅ Online player mace scanning initiated! Check console for results.")
                    .color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("💡 Use '/scanmaces all' to include offline players")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }
}