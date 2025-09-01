package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.managers.ElementManager;

public class RerollCommand implements CommandExecutor {

    private final ElementManager elementManager;

    public RerollCommand(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if command is used by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // Handle different argument cases
        switch (args.length) {
            case 0:
                // Self reroll
                handleSelfReroll(player);
                break;
            case 1:
                // Admin reroll another player
                handleAdminReroll(sender, args[0]);
                break;
            default:
                sender.sendMessage(Component.text("Usage: /reroll [player]")
                        .color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Use /reroll to reroll your own element")
                        .color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Use /reroll <player> to reroll another player's element (admin only)")
                        .color(NamedTextColor.GRAY));
                break;
        }

        return true;
    }

    /**
     * Handles a player rerolling their own element
     */
    private void handleSelfReroll(Player player) {
        String currentElement = elementManager.getPlayerElement(player);

        if (currentElement == null) {
            player.sendMessage(Component.text("❌ You don't have an element assigned yet!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Show current element
        player.sendMessage(Component.text("🎲 Current Element: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(elementManager.getElementDisplayName(currentElement))
                        .color(elementManager.getElementColor(currentElement))));

        player.sendMessage(Component.text("🎲 Rerolling your element...")
                .color(NamedTextColor.GOLD));

        // Force a new element assignment with animation
        elementManager.rerollPlayerElement(player);
    }

    /**
     * Handles an admin rerolling another player's element
     */
    private void handleAdminReroll(CommandSender sender, String targetName) {
        // Check admin permission
        if (!sender.hasPermission("element.reroll.others")) {
            sender.sendMessage(Component.text("❌ You don't have permission to reroll other players' elements!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("❌ Player '" + targetName + "' is not online!")
                    .color(NamedTextColor.RED));
            return;
        }

        String currentElement = elementManager.getPlayerElement(target);

        if (currentElement == null) {
            sender.sendMessage(Component.text("❌ " + target.getName() + " doesn't have an element assigned yet!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Notify admin and target
        sender.sendMessage(Component.text("🎲 Rerolling " + target.getName() + "'s element...")
                .color(NamedTextColor.GOLD));

        target.sendMessage(Component.text("🎲 Your element is being rerolled by an admin...")
                .color(NamedTextColor.GOLD));

        // Show current element to admin
        sender.sendMessage(Component.text("Previous Element: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(elementManager.getElementDisplayName(currentElement))
                        .color(elementManager.getElementColor(currentElement))));

        // Force a new element assignment with animation
        elementManager.rerollPlayerElement(target);

        // Notify admin when complete (delayed message)
        Bukkit.getScheduler().runTaskLater(elementManager.getPlugin(), () -> {
            String newElement = elementManager.getPlayerElement(target);
            if (newElement != null) {
                sender.sendMessage(Component.text("✅ " + target.getName() + "'s new element: ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(elementManager.getElementDisplayName(newElement))
                                .color(elementManager.getElementColor(newElement))));
            }
        }, 80L); // Wait for animation to complete
    }
}