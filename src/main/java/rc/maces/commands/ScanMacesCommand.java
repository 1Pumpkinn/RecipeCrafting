package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import rc.maces.listeners.CraftingListener;

public class ScanMacesCommand implements CommandExecutor {

    private final CraftingListener craftingListener;
    private final JavaPlugin plugin;
    private static BukkitTask scanTask = null;

    public ScanMacesCommand(CraftingListener craftingListener, JavaPlugin plugin) {
        this.craftingListener = craftingListener;
        this.plugin = plugin;
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
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("auto")) {
                if (scanTask != null && !scanTask.isCancelled()) {
                    scanTask.cancel();
                    sender.sendMessage(Component.text("⏹️ Automatic mace scanning stopped.")
                            .color(NamedTextColor.YELLOW));
                } else {
                    startAutoScan();
                    sender.sendMessage(Component.text("▶️ Automatic mace scanning started! Players will be scanned every 5 minutes.")
                            .color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("Use '/scanmaces auto' again to stop automatic scanning.")
                            .color(NamedTextColor.GRAY));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("stop")) {
                if (scanTask != null && !scanTask.isCancelled()) {
                    scanTask.cancel();
                    sender.sendMessage(Component.text("⏹️ Automatic mace scanning stopped.")
                            .color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("❌ No automatic scanning is currently running.")
                            .color(NamedTextColor.RED));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
                // Manual override: /scanmaces set <mace_type> <player_name>
                String maceType = args[1].toUpperCase();
                String playerName = args[2];

                if (!isValidMaceType(maceType)) {
                    sender.sendMessage(Component.text("❌ Invalid mace type! Use: AIR, FIRE, WATER, or EARTH")
                            .color(NamedTextColor.RED));
                    return true;
                }

                craftingListener.setGlobalMaceCrafted(maceType, playerName);
                sender.sendMessage(Component.text("✅ Manually set " + maceType.toLowerCase() + " mace as crafted by " + playerName)
                        .color(NamedTextColor.GREEN));
                return true;
            } else if (args[0].equalsIgnoreCase("reset") && args.length >= 2) {
                // Manual reset: /scanmaces reset <mace_type>
                String maceType = args[1].toUpperCase();

                if (!isValidMaceType(maceType)) {
                    sender.sendMessage(Component.text("❌ Invalid mace type! Use: AIR, FIRE, WATER, or EARTH")
                            .color(NamedTextColor.RED));
                    return true;
                }

                craftingListener.resetGlobalMaceStatus(maceType);
                sender.sendMessage(Component.text("✅ Reset " + maceType.toLowerCase() + " mace status to available")
                        .color(NamedTextColor.GREEN));
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                showHelp(sender);
                return true;
            }
        }

        // Manual scan
        sender.sendMessage(Component.text("🔍 Scanning online players for existing maces...")
                .color(NamedTextColor.YELLOW));

        // Trigger the scan
        craftingListener.scanAllPlayersCommand();

        sender.sendMessage(Component.text("✅ Online player mace scanning completed! Check console for results.")
                .color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("💡 Use '/scanmaces help' to see all available options")
                .color(NamedTextColor.GRAY));

        return true;
    }

    private void showHelp(CommandSender sender) {
        Component helpMessage = Component.text("=== SCANMACES COMMAND HELP ===")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("/scanmaces").color(NamedTextColor.GREEN))
                .append(Component.text(" - Manual scan of all online players").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/scanmaces auto").color(NamedTextColor.GREEN))
                .append(Component.text(" - Toggle automatic scanning every 5 minutes").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/scanmaces stop").color(NamedTextColor.GREEN))
                .append(Component.text(" - Stop automatic scanning").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/scanmaces set <type> <player>").color(NamedTextColor.GREEN))
                .append(Component.text(" - Manually mark a mace as crafted").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/scanmaces reset <type>").color(NamedTextColor.GREEN))
                .append(Component.text(" - Reset a mace to available status").color(NamedTextColor.GRAY))
                .appendNewline()
                .appendNewline()
                .append(Component.text("Mace types: ").color(NamedTextColor.YELLOW))
                .append(Component.text("AIR, FIRE, WATER, EARTH").color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Examples:").color(NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("  /scanmaces set FIRE Steve").color(NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("  /scanmaces reset WATER").color(NamedTextColor.AQUA));

        sender.sendMessage(helpMessage);
    }

    private boolean isValidMaceType(String maceType) {
        return maceType.equals("AIR") || maceType.equals("FIRE") ||
                maceType.equals("WATER") || maceType.equals("EARTH");
    }

    private void startAutoScan() {
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Scan all online players
                craftingListener.scanAllPlayersCommand();

                // Send message to all ops
                Component message = Component.text("🔄 Automatic mace scan completed for " +
                                Bukkit.getOnlinePlayers().size() + " online players.")
                        .color(NamedTextColor.AQUA);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp()) {
                        player.sendMessage(message);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 6000L); // Run immediately, then every 5 minutes (6000 ticks)
    }

    public static void stopAutoScan() {
        if (scanTask != null && !scanTask.isCancelled()) {
            scanTask.cancel();
        }
    }
}