package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.ElementManager;

public class ElementCommand implements CommandExecutor {

    private final ElementManager elementManager;

    public ElementCommand(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // If no arguments, show list of all elements
        if (args.length == 0) {
            player.sendMessage(elementManager.getElementList());
            return true;
        }

        // If argument provided, show specific element info
        String elementArg = args[0].toUpperCase();

        if (elementManager.isValidElement(elementArg)) {
            player.sendMessage(elementManager.getDetailedElementInfo(elementArg));
        } else {
            player.sendMessage(Component.text("❌ Invalid element! Valid elements are: fire, water, earth, air")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use /element without arguments to see all elements!")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }
}