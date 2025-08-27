package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.TrustManager;

public class TrustDebugCommand implements CommandExecutor {

    private final TrustManager trustManager;

    public TrustDebugCommand(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("❌ You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                showTrustStatus(sender);
                break;
            case "check":
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Usage: /trustdebug check <player1> <player2>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                checkTrustBetweenPlayers(sender, args[1], args[2]);
                break;
            case "test":
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Usage: /trustdebug test <caster> <target>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                testAbilityPermission(sender, args[1], args[2]);
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══════ TRUST DEBUG COMMANDS ═══════")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/trustdebug status")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show overall trust system status")
                        .color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/trustdebug check <player1> <player2>")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Check if two players trust each other")
                        .color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/trustdebug test <caster> <target>")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Test if caster can use abilities on target")
                        .color(NamedTextColor.GRAY)));
    }

    private void showTrustStatus(CommandSender sender) {
        sender.sendMessage(Component.text("═══════ TRUST SYSTEM STATUS ═══════")
                .color(NamedTextColor.GOLD));

        sender.sendMessage(Component.text("Total Trust Relationships: " + trustManager.getTotalTrustRelationships())
                .color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Pending Requests: " + trustManager.getPendingRequestCount())
                .color(NamedTextColor.YELLOW));

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("Online Players with Allies:")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true));

        for (Player player : Bukkit.getOnlinePlayers()) {
            var trustedPlayers = trustManager.getTrustedPlayers(player);
            if (!trustedPlayers.isEmpty()) {
                sender.sendMessage(Component.text("• " + player.getName() + ": " + trustedPlayers.size() + " allies")
                        .color(NamedTextColor.GREEN));
                for (String trusted : trustedPlayers) {
                    sender.sendMessage(Component.text("  - " + trusted)
                            .color(NamedTextColor.GRAY));
                }
            }
        }
    }

    private void checkTrustBetweenPlayers(CommandSender sender, String player1Name, String player2Name) {
        Player player1 = Bukkit.getPlayer(player1Name);
        Player player2 = Bukkit.getPlayer(player2Name);

        if (player1 == null) {
            sender.sendMessage(Component.text("❌ Player '" + player1Name + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (player2 == null) {
            sender.sendMessage(Component.text("❌ Player '" + player2Name + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══════ TRUST CHECK ═══════")
                .color(NamedTextColor.GOLD));

        boolean areTrusted = trustManager.isTrusted(player1, player2);

        sender.sendMessage(Component.text("Players: " + player1.getName() + " ↔ " + player2.getName())
                .color(NamedTextColor.YELLOW));

        if (areTrusted) {
            sender.sendMessage(Component.text("✅ STATUS: TRUSTED ALLIES")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
            sender.sendMessage(Component.text("• They are protected from each other's abilities")
                    .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("❌ STATUS: NOT ALLIED")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
            sender.sendMessage(Component.text("• They can affect each other with abilities")
                    .color(NamedTextColor.RED));
        }
    }

    private void testAbilityPermission(CommandSender sender, String casterName, String targetName) {
        Player caster = Bukkit.getPlayer(casterName);
        Player target = Bukkit.getPlayer(targetName);

        if (caster == null) {
            sender.sendMessage(Component.text("❌ Player '" + casterName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (target == null) {
            sender.sendMessage(Component.text("❌ Player '" + targetName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══════ ABILITY PERMISSION TEST ═══════")
                .color(NamedTextColor.GOLD));

        boolean canUseAbility = trustManager.canUseAbilityOn(caster, target);

        sender.sendMessage(Component.text("Caster: " + caster.getName())
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Target: " + target.getName())
                .color(NamedTextColor.YELLOW));

        if (canUseAbility) {
            sender.sendMessage(Component.text("✅ RESULT: ABILITY ALLOWED")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
            sender.sendMessage(Component.text("• " + caster.getName() + " CAN use abilities on " + target.getName())
                    .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("🛡️ RESULT: ABILITY BLOCKED")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
            sender.sendMessage(Component.text("• " + caster.getName() + " CANNOT use abilities on " + target.getName())
                    .color(NamedTextColor.RED));
            sender.sendMessage(Component.text("• Protected by alliance system")
                    .color(NamedTextColor.GOLD));
        }
    }
}