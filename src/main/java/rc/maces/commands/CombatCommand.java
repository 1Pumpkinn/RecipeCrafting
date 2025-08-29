package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.CombatTimer;

import java.util.Map;
import java.util.UUID;

public class CombatCommand implements CommandExecutor {

    private final CombatTimer combatTimer;

    public CombatCommand(CombatTimer combatTimer) {
        this.combatTimer = combatTimer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // Handle subcommands for admins
        if (args.length > 0 && player.hasPermission("element.admin")) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "list":
                    listAllCombatPlayers(player);
                    return true;
                case "remove":
                    if (args.length != 2) {
                        player.sendMessage(Component.text("Usage: /combat remove <player>")
                                .color(NamedTextColor.RED));
                        return true;
                    }
                    removePlayerFromCombat(player, args[1]);
                    return true;
                case "force":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Usage: /combat force <player> [reason]")
                                .color(NamedTextColor.RED));
                        return true;
                    }
                    String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Admin command";
                    forcePlayerIntoCombat(player, args[1], reason);
                    return true;
                case "help":
                    showAdminHelp(player);
                    return true;
                default:
                    break;
            }
        }

        // Default behavior: show combat status
        combatTimer.showCombatStatus(player);
        return true;
    }

    private void listAllCombatPlayers(Player admin) {
        Map<UUID, Long> combatPlayers = combatTimer.getCombatPlayers();

        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        admin.sendMessage(Component.text("     PLAYERS IN COMBAT")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));

        if (combatPlayers.isEmpty()) {
            admin.sendMessage(Component.text("No players are currently in combat.")
                    .color(NamedTextColor.GRAY));
        } else {
            admin.sendMessage(Component.text("Total players in combat: " + combatPlayers.size())
                    .color(NamedTextColor.YELLOW));
            admin.sendMessage(Component.text(""));

            int count = 1;
            for (UUID playerId : combatPlayers.keySet()) {
                Player combatPlayer = Bukkit.getPlayer(playerId);
                if (combatPlayer != null && combatPlayer.isOnline()) {
                    String timeLeft = combatTimer.getRemainingCombatTimeFormatted(combatPlayer);
                    Player cause = combatTimer.getCombatCause(combatPlayer);
                    String causeText = cause != null ? " (vs " + cause.getName() + ")" : "";

                    admin.sendMessage(Component.text(count + ". " + combatPlayer.getName() + " - " + timeLeft + causeText)
                            .color(NamedTextColor.RED));
                    count++;
                } else {
                    admin.sendMessage(Component.text(count + ". Unknown Player - " + playerId.toString().substring(0, 8))
                            .color(NamedTextColor.DARK_GRAY));
                    count++;
                }
            }
        }

        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
    }

    private void removePlayerFromCombat(Player admin, String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            admin.sendMessage(Component.text("❌ Player '" + targetName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (!combatTimer.isInCombat(target)) {
            admin.sendMessage(Component.text("❌ " + target.getName() + " is not in combat!")
                    .color(NamedTextColor.RED));
            return;
        }

        combatTimer.removeFromCombat(target);
        admin.sendMessage(Component.text("✅ Removed " + target.getName() + " from combat.")
                .color(NamedTextColor.GREEN));

        // Log the admin action
        admin.getServer().getLogger().info("Admin " + admin.getName() + " removed " + target.getName() + " from combat");
    }

    private void forcePlayerIntoCombat(Player admin, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            admin.sendMessage(Component.text("❌ Player '" + targetName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (combatTimer.isInCombat(target)) {
            admin.sendMessage(Component.text("❌ " + target.getName() + " is already in combat!")
                    .color(NamedTextColor.RED));
            return;
        }

        combatTimer.forceInCombat(target, admin, reason);
        admin.sendMessage(Component.text("✅ Forced " + target.getName() + " into combat.")
                .color(NamedTextColor.GREEN));

        // Log the admin action
        admin.getServer().getLogger().info("Admin " + admin.getName() + " forced " + target.getName() + " into combat: " + reason);
    }

    private void showAdminHelp(Player admin) {
        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        admin.sendMessage(Component.text("     COMBAT ADMIN COMMANDS")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));

        admin.sendMessage(Component.text("/combat")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Check your combat status")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat list")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - List all players in combat")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat remove <player>")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a player from combat")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat force <player> [reason]")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Force a player into combat")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat protection <player>")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Check player's protection status")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat safezone")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show safe zone information")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("/combat help")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show this help menu")
                        .color(NamedTextColor.GRAY)));

        admin.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
    }
}