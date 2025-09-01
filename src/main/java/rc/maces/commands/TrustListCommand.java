package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.TrustManager;

import java.util.List;

public class TrustListCommand implements CommandExecutor {

    private final TrustManager trustManager;

    public TrustListCommand(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        List<String> trustedPlayers = trustManager.getTrustedPlayers(player);

        if (trustedPlayers.isEmpty()) {
            player.sendMessage(Component.text("═══════════════════════════════════")
                    .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("           TRUSTED ALLIES")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("═══════════════════════════════════")
                    .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("You have no trusted allies.")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Use /trust <player> to send a trust request!")
                    .color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("═══════════════════════════════════")
                    .color(NamedTextColor.GOLD));
            return true;
        }

        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("           TRUSTED ALLIES")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Total Allies: " + trustedPlayers.size())
                        .color(NamedTextColor.YELLOW))
                .appendNewline();

        for (int i = 0; i < trustedPlayers.size(); i++) {
            String playerName = trustedPlayers.get(i);
            message = message.append(Component.text((i + 1) + ". " + playerName)
                            .color(NamedTextColor.GREEN))
                    .appendNewline();
        }

        message = message.append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Use /untrust <player> to remove an ally")
                        .color(NamedTextColor.GRAY));

        player.sendMessage(message);
        return true;
    }
}