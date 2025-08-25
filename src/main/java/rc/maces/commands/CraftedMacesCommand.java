package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

public class CraftedMacesCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final MaceManager maceManager;

    public CraftedMacesCommand(ElementManager elementManager, MaceManager maceManager) {
        this.elementManager = elementManager;
        this.maceManager = maceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String playerElement = elementManager.getPlayerElement(player);

        if (playerElement == null) {
            player.sendMessage(Component.text("❌ You don't have an element assigned yet!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // For now, we'll show a placeholder since we need to implement mace tracking
        // This can be enhanced later to actually track crafted maces
        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("           CRAFTED MACES")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Air: ")
                        .color(NamedTextColor.WHITE))
                .append(Component.text("0/1")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Water: ")
                        .color(NamedTextColor.BLUE))
                .append(Component.text("0/1")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Earth: ")
                        .color(NamedTextColor.GREEN))
                .append(Component.text("0/1")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("Fire: ")
                        .color(NamedTextColor.RED))
                .append(Component.text("0/1")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Note: Mace tracking coming soon!")
                        .color(NamedTextColor.YELLOW));

        player.sendMessage(message);
        return true;
    }
}
