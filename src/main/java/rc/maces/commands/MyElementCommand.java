package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

public class MyElementCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final MaceManager maceManager;

    public MyElementCommand(ElementManager elementManager, MaceManager maceManager) {
        this.elementManager = elementManager;
        this.maceManager = maceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        String playerElement = elementManager.getPlayerElement(player);

        if (playerElement == null) {
            player.sendMessage(Component.text("❌ You don't have an element assigned!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Check if player has a mace crafted
        boolean hasMaceCrafted = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (maceManager.isCustomMace(item)) {
                // Check if the mace matches their element
                if ((playerElement.equals("AIR") && maceManager.isAirMace(item)) ||
                        (playerElement.equals("FIRE") && maceManager.isFireMace(item)) ||
                        (playerElement.equals("WATER") && maceManager.isWaterMace(item)) ||
                        (playerElement.equals("EARTH") && maceManager.isEarthMace(item))) {
                    hasMaceCrafted = true;
                    break;
                }
            }
        }

        // Display element information in improved format
        displayElementInfo(player, playerElement, hasMaceCrafted);

        return true;
    }

    private void displayElementInfo(Player player, String element, boolean hasMaceCrafted) {
        NamedTextColor elementColor = elementManager.getElementColor(element);
        String elementDisplay = elementManager.getElementDisplayName(element);

        // Header with separator
        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));

        // Element name with icon
        String elementIcon = getElementIcon(element);
        player.sendMessage(Component.text(elementIcon + " " + elementDisplay + " Element " + elementIcon)
                .color(elementColor)
                .decoration(TextDecoration.BOLD, true));

        player.sendMessage(Component.text("═══════════════════════════════════════")
                .color(NamedTextColor.DARK_GRAY));

        // Passive abilities section
        player.sendMessage(Component.text("PASSIVE ABILITIES:")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));

        switch (element) {
            case "AIR":
                player.sendMessage(Component.text("  • Speed I")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("  • Fall Damage Immunity")
                        .color(NamedTextColor.WHITE));
                break;
            case "FIRE":
                player.sendMessage(Component.text("  • Fire Resistance")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("  • +1 Attack Damage when on fire")
                        .color(NamedTextColor.RED));
                break;
            case "WATER":
                player.sendMessage(Component.text("  • Dolphins Grace I (in water)")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("  • Conduit Power")
                        .color(NamedTextColor.BLUE));
                break;
            case "EARTH":
                player.sendMessage(Component.text("  • Haste III")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("  • Hero of the Village I")
                        .color(NamedTextColor.GREEN));
                break;
        }

        player.sendMessage(Component.empty());

        // Mace status section
        String maceStatus = hasMaceCrafted ? "✓ CRAFTED" : "✗ NOT CRAFTED";
        NamedTextColor statusColor = hasMaceCrafted ? NamedTextColor.GREEN : NamedTextColor.RED;

        player.sendMessage(Component.text("MACE STATUS: ")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(maceStatus)
                        .color(statusColor)
                        .decoration(TextDecoration.BOLD, true)));

        player.sendMessage(Component.empty());

        // Mace abilities section
        player.sendMessage(Component.text("MACE ABILITIES:")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));

        switch (element) {
            case "AIR":
                player.sendMessage(Component.text("  Right-Click: Wind Shot")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("    └ Shoots a wind charge")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  F Key: Wind Struck")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("    └ Traps enemies in cobwebs with slow falling")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Passive: Enhanced wind charge pulling")
                        .color(NamedTextColor.WHITE));
                break;
            case "FIRE":
                player.sendMessage(Component.text("  Right-Click: Obsidian Creation")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("    └ Converts water to obsidian around enemies")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  F Key: Meteors")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("    └ Rain 15 meteors in 8x8 area")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Passive: Ignite enemies on hit, fire immunity")
                        .color(NamedTextColor.RED));
                break;
            case "WATER":
                player.sendMessage(Component.text("  Right-Click: Water Heal")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("    └ Heals 2 hearts")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  F Key: Water Geyser")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("    └ Launch nearby enemies upward")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Passive: 1% chance to give mining fatigue on hit")
                        .color(NamedTextColor.BLUE));
                break;
            case "EARTH":
                player.sendMessage(Component.text("  Right-Click: Buddy Up")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("    └ Summons protective iron golem")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  F Key: Vine Trap")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("    └ Immobilizes enemies for 5 seconds")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Passive: Food acts like golden apples, Haste V when holding")
                        .color(NamedTextColor.GREEN));
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