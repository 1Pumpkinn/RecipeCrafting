package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.ElementManager;

public class MyElementCommand implements CommandExecutor {

    private final ElementManager elementManager;

    public MyElementCommand(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String element = elementManager.getPlayerElement(player);

        if (element == null) {
            player.sendMessage(Component.text("❌ You don't have an element assigned yet!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Create the element info message
        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("           YOUR ELEMENT")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Element: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(elementManager.getElementDisplayName(element))
                        .color(elementManager.getElementColor(element))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("Mace Type: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(element.toLowerCase() + " mace")
                        .color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Power: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(getElementPower(element))
                        .color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD));

        player.sendMessage(message);
        return true;
    }

    private String getElementPower(String element) {
        switch (element) {
            case "FIRE":
                return "Fire Rest - Immune to fire damage and gain strength when on fire";
            case "WATER":
                return "Dolphin's Grace - Enhanced swimming speed and water breathing";
            case "EARTH":
                return "Hero of the Village 1 - Enhanced trading and village benefits";
            case "AIR":
                return "Speed 1 - Enhanced movement and wind control";
            default:
                return "Unknown power";
        }
    }
}
