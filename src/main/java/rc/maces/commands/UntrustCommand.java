package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.TrustManager;

public class UntrustCommand implements CommandExecutor {

    private final TrustManager trustManager;

    public UntrustCommand(TrustManager trustManager) {
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

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /untrust <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("❌ Player '" + args[0] + "' is not online!")
                    .color(NamedTextColor.RED));
            return true;
        }

        trustManager.removeTrust(player, target);
        return true;
    }
}