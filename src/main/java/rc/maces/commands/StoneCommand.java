package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.ElementManager;

public class StoneCommand implements CommandExecutor {

    private final ElementManager elementManager;

    public StoneCommand(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has permission (adjust permission as needed)
        if (!sender.hasPermission("maces.stone") && !sender.isOp()) {
            sender.sendMessage(Component.text("❌ You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /stone <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("❌ Player not found: " + args[0])
                    .color(NamedTextColor.RED));
            return true;
        }

        // Check if they already have Stone element
        String currentElement = elementManager.getPlayerElement(target);
        if ("STONE".equals(currentElement)) {
            sender.sendMessage(Component.text("❌ " + target.getName() + " already has the Stone element!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Assign Stone element
        elementManager.assignStoneElement(target);

        sender.sendMessage(Component.text("✅ Awarded Stone element to " + target.getName() + "!")
                .color(NamedTextColor.GREEN));

        // Announce to server
        Bukkit.broadcast(Component.text("🗿 " + target.getName() + " has been awarded the legendary Stone Element! 🗿")
                .color(NamedTextColor.GOLD));

        return true;
    }
}