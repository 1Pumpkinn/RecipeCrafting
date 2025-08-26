package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import rc.maces.listeners.CraftingRestrictionListener;
import rc.maces.listeners.HeavyCoreMonitor;

public class MaceSecurityCommand implements CommandExecutor {

    private final CraftingRestrictionListener craftingRestrictionListener;
    private final HeavyCoreMonitor heavyCoreMonitor;

    public MaceSecurityCommand(CraftingRestrictionListener craftingRestrictionListener, HeavyCoreMonitor heavyCoreMonitor) {
        this.craftingRestrictionListener = craftingRestrictionListener;
        this.heavyCoreMonitor = heavyCoreMonitor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("❌ You don't have permission to use security commands!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showSecurityStatus(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("scan")) {
            performSecurityScan(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        sender.sendMessage(Component.text("❌ Unknown security command. Use /macesecurity help")
                .color(NamedTextColor.RED));
        return true;
    }

    private void showSecurityStatus(CommandSender sender) {
        int monitoredCrafters = craftingRestrictionListener.getMonitoredCrafterCount();
        int activelyMonitored = heavyCoreMonitor.getActivelyMonitoredCount();

        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("      MACE SECURITY STATUS")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("🛡️ Protection Level: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text("MAXIMUM")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .appendNewline()
                .append(Component.text("📊 MONITORING STATISTICS:")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("• Total Monitored Crafters: ")
                        .color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(monitoredCrafters))
                        .color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("• Actively Monitored: ")
                        .color(NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(activelyMonitored))
                        .color(NamedTextColor.YELLOW))
                .appendNewline()
                .appendNewline()
                .append(Component.text("✅ ACTIVE PROTECTIONS:")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("• Player Crafting: BLOCKED")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("• Hopper Automation: BLOCKED")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("• Crafter Automation: BLOCKED")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("• Dispenser Output: BLOCKED")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("• Heavy Core Tracking: ACTIVE")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("• Recipe Pattern Detection: ACTIVE")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD));

        sender.sendMessage(message);
    }

    private void performSecurityScan(CommandSender sender) {
        sender.sendMessage(Component.text("🔍 Performing security scan...")
                .color(NamedTextColor.YELLOW));

        // Force check all monitored crafters
        craftingRestrictionListener.forceCheckAllCrafters();

        sender.sendMessage(Component.text("✅ Security scan complete! Check console for any violations.")
                .color(NamedTextColor.GREEN));
    }

    private void showHelp(CommandSender sender) {
        Component help = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("        MACE SECURITY HELP")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("/macesecurity status")
                        .color(NamedTextColor.AQUA))
                .append(Component.text(" - Show security status")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/macesecurity scan")
                        .color(NamedTextColor.AQUA))
                .append(Component.text(" - Force security scan")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/macesecurity help")
                        .color(NamedTextColor.AQUA))
                .append(Component.text(" - Show this help")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD));

        sender.sendMessage(help);
    }
}