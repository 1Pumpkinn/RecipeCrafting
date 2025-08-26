package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            showElementList(player);
            return true;
        }

        // If argument provided, show specific element info or handle admin commands
        String elementArg = args[0].toUpperCase();

        // Admin commands (if player has permission)
        if (player.hasPermission("maces.admin") || player.isOp()) {
            if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
                // /element set <player> <element>
                String targetName = args[1];
                String newElement = args[2].toUpperCase();

                Player target = player.getServer().getPlayer(targetName);
                if (target == null) {
                    player.sendMessage(Component.text("❌ Player not found: " + targetName)
                            .color(NamedTextColor.RED));
                    return true;
                }

                if (!elementManager.isValidElement(newElement)) {
                    player.sendMessage(Component.text("❌ Invalid element! Valid elements are: fire, water, earth, air")
                            .color(NamedTextColor.RED));
                    return true;
                }

                elementManager.setPlayerElement(target, newElement);
                player.sendMessage(Component.text("✅ Set " + target.getName() + "'s element to " +
                                elementManager.getElementDisplayName(newElement))
                        .color(NamedTextColor.GREEN));
                target.sendMessage(Component.text("⚡ Your element has been set to " +
                                elementManager.getElementDisplayName(newElement) + "!")
                        .color(elementManager.getElementColor(newElement)));
                return true;
            }

            if (args.length >= 2 && args[0].equalsIgnoreCase("get")) {
                // /element get <player>
                String targetName = args[1];
                Player target = player.getServer().getPlayer(targetName);
                if (target == null) {
                    player.sendMessage(Component.text("❌ Player not found: " + targetName)
                            .color(NamedTextColor.RED));
                    return true;
                }

                String targetElement = elementManager.getPlayerElement(target);
                if (targetElement == null) {
                    player.sendMessage(Component.text(target.getName() + " has no element assigned")
                            .color(NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text(target.getName() + "'s element is: " +
                                    elementManager.getElementDisplayName(targetElement))
                            .color(elementManager.getElementColor(targetElement)));
                }
                return true;
            }
        }

        // Show specific element info
        if (elementManager.isValidElement(elementArg)) {
            showElementInfo(player, elementArg);
        } else {
            player.sendMessage(Component.text("❌ Invalid element! Valid elements are: fire, water, earth, air")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use /element without arguments to see all elements!")
                    .color(NamedTextColor.GRAY));

            if (player.hasPermission("maces.admin") || player.isOp()) {
                player.sendMessage(Component.text("Admin commands:")
                        .color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("  /element set <player> <element>")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  /element get <player>")
                        .color(NamedTextColor.GRAY));
            }
        }

        return true;
    }

    private void showElementList(Player player) {
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("ELEMENTAL MACES SYSTEM")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));

        // Air Element
        player.sendMessage(Component.text("💨 AIR ELEMENT")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Passives: Speed I, Fall Damage Immunity")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Use /element air for more details")
                .color(NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.empty());

        // Fire Element
        player.sendMessage(Component.text("🔥 FIRE ELEMENT")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Passives: Fire Resistance, +1 Attack when on fire")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Use /element fire for more details")
                .color(NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.empty());

        // Water Element
        player.sendMessage(Component.text("🌊 WATER ELEMENT")
                .color(NamedTextColor.BLUE)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Passives: Dolphins Grace in water, Conduit Power")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Use /element water for more details")
                .color(NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.empty());

        // Earth Element
        player.sendMessage(Component.text("🌍 EARTH ELEMENT")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("  Passives: Haste III, Hero of the Village")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Use /element earth for more details")
                .color(NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("Use /myelement to see your current element and abilities!")
                .color(NamedTextColor.YELLOW));
    }

    private void showElementInfo(Player player, String element) {
        NamedTextColor elementColor = elementManager.getElementColor(element);
        String elementDisplay = elementManager.getElementDisplayName(element);
        String elementIcon = getElementIcon(element);

        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text(elementIcon + " " + elementDisplay + " ELEMENT " + elementIcon)
                .color(elementColor)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));

        // Show detailed info for each element
        switch (element) {
            case "AIR":
                player.sendMessage(Component.text("PASSIVE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Speed I - Move faster")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("• Fall Damage Immunity - No fall damage")
                        .color(NamedTextColor.WHITE));

                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("MACE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Wind Shot (Right-Click) - Launch wind charge")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("• Wind Struck (F Key) - Trap enemies in cobwebs")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("• Enhanced wind charge pulling when holding mace")
                        .color(NamedTextColor.GRAY));
                break;

            case "FIRE":
                player.sendMessage(Component.text("PASSIVE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Fire Resistance - Immune to fire damage")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("• +1 Attack Damage when on fire")
                        .color(NamedTextColor.RED));

                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("MACE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Obsidian Creation (Right-Click) - Convert water to obsidian")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("• Meteors (F Key) - Rain 15 meteors in 8x8 area")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("• Ignite enemies on hit")
                        .color(NamedTextColor.GRAY));
                break;

            case "WATER":
                player.sendMessage(Component.text("PASSIVE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Dolphins Grace I in water - Swim faster")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("• Conduit Power - See underwater, breathe")
                        .color(NamedTextColor.BLUE));

                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("MACE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Water Heal (Right-Click) - Restore 2 hearts")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("• Water Geyser (F Key) - Launch enemies upward")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("• 1% chance to inflict mining fatigue on hit")
                        .color(NamedTextColor.GRAY));
                break;

            case "EARTH":
                player.sendMessage(Component.text("PASSIVE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Haste III - Mine faster")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("• Hero of the Village I - Better villager trades")
                        .color(NamedTextColor.GREEN));

                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("MACE ABILITIES:")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text("• Buddy Up (Right-Click) - Summon protective golem")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("• Vine Trap (F Key) - Immobilize enemies for 5s")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("• Food acts like golden apples when holding mace")
                        .color(NamedTextColor.GRAY));
                break;
        }

        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));
    }

    private String getElementIcon(String element) {
        switch (element) {
            case "AIR": return "💨";
            case "FIRE": return "🔥";
            case "WATER": return "🌊";
            case "EARTH": return "🌍";
            default: return "⭐";
        }
    }
}