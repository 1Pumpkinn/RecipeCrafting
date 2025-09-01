package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.MaceManager;

public class FiremaceCommand implements CommandExecutor {

    private final MaceManager maceManager;

    public FiremaceCommand(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("firemace.give")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /firemace <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' is not online!")
                    .color(NamedTextColor.RED));
            return true;
        }

        maceManager.giveFireMace(target);
        sender.sendMessage(Component.text("You have given " + target.getName() + " the Fire Mace!")
                .color(NamedTextColor.LIGHT_PURPLE));
        target.sendMessage(Component.text("You have been given the Fire Mace!")
                .color(NamedTextColor.LIGHT_PURPLE));

        return true;
    }
}