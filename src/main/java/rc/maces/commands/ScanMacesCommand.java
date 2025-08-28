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
            }
        }

        // Manual scan
        sender.sendMessage(Component.text("🔍 Scanning online players for existing maces...")
                .color(NamedTextColor.YELLOW));

        // Trigger the scan
        craftingListener.scanAllPlayersCommand();

        sender.sendMessage(Component.text("✅ Online player mace scanning completed! Check console for results.")
                .color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("💡 Use '/scanmaces auto' to enable automatic scanning every 5 minutes")
                .color(NamedTextColor.GRAY));

        return true;
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