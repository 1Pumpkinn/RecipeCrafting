package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /element <check|set> [player] [element]")
                    .color(NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "check":
                if (args.length != 2) {
                    sender.sendMessage(Component.text("Usage: /element check <player>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                handleCheckCommand(sender, args[1]);
                break;

            case "set":
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Usage: /element set <player> <fire|water|earth|air>")
                            .color(NamedTextColor.RED));
                    return true;
                }
                handleSetCommand(sender, args[1], args[2]);
                break;

            default:
                sender.sendMessage(Component.text("Unknown action! Use: check, set")
                        .color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void handleCheckCommand(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + playerName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        String element = elementManager.getPlayerElement(target);
        if (element == null) {
            sender.sendMessage(Component.text(target.getName() + " has no element assigned!")
                    .color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text(target.getName() + "'s element: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(elementManager.getElementDisplayName(element))
                            .color(elementManager.getElementColor(element))));
        }
    }

    private void handleSetCommand(CommandSender sender, String playerName, String element) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + playerName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (!elementManager.isValidElement(element)) {
            sender.sendMessage(Component.text("Invalid element! Use: fire, water, earth, or air")
                    .color(NamedTextColor.RED));
            return;
        }

        elementManager.setPlayerElement(target, element);
        sender.sendMessage(Component.text("Set " + target.getName() + "'s element to ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(elementManager.getElementDisplayName(element.toUpperCase()))
                        .color(elementManager.getElementColor(element.toUpperCase()))));
    }
}