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
            sender.sendMessage(Component.text("Usage: /element <check|set|info> [player] [element]")
                    .color(NamedTextColor.RED));
            sender.sendMessage(Component.text("• /element check <player> - Check a player's element")
                    .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("• /element set <player> <element> - Set a player's element")
                    .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("• /element info [element] - Show element information")
                    .color(NamedTextColor.GRAY));
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

            case "info":
                if (args.length == 1) {
                    handleInfoAllCommand(sender);
                } else if (args.length == 2) {
                    handleInfoSpecificCommand(sender, args[1]);
                } else {
                    sender.sendMessage(Component.text("Usage: /element info [element]")
                            .color(NamedTextColor.RED));
                }
                break;

            default:
                sender.sendMessage(Component.text("Unknown action! Use: check, set, info")
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

    private void handleInfoAllCommand(CommandSender sender) {
        sender.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("           ELEMENT INFORMATION")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));

        // Show all elements
        showElementInfo(sender, "FIRE");
        sender.sendMessage(Component.text(""));
        showElementInfo(sender, "WATER");
        sender.sendMessage(Component.text(""));
        showElementInfo(sender, "EARTH");
        sender.sendMessage(Component.text(""));
        showElementInfo(sender, "AIR");

        sender.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Use /element info <element> for detailed info")
                .color(NamedTextColor.GRAY));
    }

    private void handleInfoSpecificCommand(CommandSender sender, String element) {
        if (!elementManager.isValidElement(element)) {
            sender.sendMessage(Component.text("Invalid element! Use: fire, water, earth, or air")
                    .color(NamedTextColor.RED));
            return;
        }

        String upperElement = element.toUpperCase();
        sender.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
        showElementInfo(sender, upperElement);
        sender.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.GOLD));
    }

    private void showElementInfo(CommandSender sender, String element) {
        NamedTextColor elementColor = elementManager.getElementColor(element);
        String displayName = elementManager.getElementDisplayName(element);

        sender.sendMessage(Component.text("🔸 Element: ")
                .color(NamedTextColor.WHITE)
                .append(Component.text(displayName)
                        .color(elementColor)));

        // Element-specific information based on actual abilities
        switch (element) {
            case "FIRE":
                sender.sendMessage(Component.text("   ⚔️ Abilities: Obsidian Creation, Meteors")
                        .color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("   🔥 Obsidian Creation (15s CD): Turn water/lava to obsidian in 4x4")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   ☄️ Meteors (25s CD): Rain 15 meteors in 7x7 area")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   💪 Strengths: Terrain control, massive area damage")
                        .color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("   🛡️ Weaknesses: Long cooldowns, requires water/lava nearby")
                        .color(NamedTextColor.RED));
                break;

            case "WATER":
                sender.sendMessage(Component.text("   ⚔️ Abilities: Water Heal, Water Geyser")
                        .color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("   🌊 Water Heal (10s CD): Restore 2 hearts instantly")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   🌊 Water Geyser (30s CD): Launch enemies upward for 2s")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   💪 Strengths: Self-healing, crowd control, displacement")
                        .color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("   🛡️ Weaknesses: Limited offensive damage, fire vulnerability")
                        .color(NamedTextColor.RED));
                break;

            case "EARTH":
                sender.sendMessage(Component.text("   ⚔️ Abilities: Buddy Up, Vine Pull")
                        .color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("   🤖 Buddy Up (15s CD): Summon protective iron golem")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   🌿 Vine Pull (20s CD): Pull & immobilize enemies for 3s")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   💪 Strengths: Tank support, crowd control, immobilization")
                        .color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("   🛡️ Weaknesses: Relies on positioning and timing")
                        .color(NamedTextColor.RED));
                break;

            case "AIR":
                sender.sendMessage(Component.text("   ⚔️ Abilities: Wind Shot, Wind Struck")
                        .color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text("   💨 Wind Shot (5s CD): Launch wind charge projectile")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   💨 Wind Struck (20s CD): Trap enemies in webs + slow fall")
                        .color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("   💪 Strengths: Fast attacks, mobility control, area denial")
                        .color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("   🛡️ Weaknesses: Lower direct damage, earth terrain blocks")
                        .color(NamedTextColor.RED));
                break;
        }
    }
}