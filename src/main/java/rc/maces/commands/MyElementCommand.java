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

        // Display element information in new format
        displayElementInfo(player, playerElement, hasMaceCrafted);

        return true;
    }

    private void displayElementInfo(Player player, String element, boolean hasMaceCrafted) {
        NamedTextColor elementColor = elementManager.getElementColor(element);
        String elementDisplay = elementManager.getElementDisplayName(element);

        // Header with element name
        player.sendMessage(Component.text("**" + elementDisplay + " Element**")
                .color(elementColor)
                .decoration(TextDecoration.BOLD, true));

        // Element-specific passive abilities
        switch (element) {
            case "AIR":
                player.sendMessage(Component.text("Speed 1")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("Fall Damage Immunity")
                        .color(NamedTextColor.GRAY));
                break;
            case "FIRE":
                player.sendMessage(Component.text("Fire Resistance")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("+1 Attack Damage when on fire")
                        .color(NamedTextColor.GRAY));
                break;
            case "WATER":
                player.sendMessage(Component.text("Dolphins Grace 1 (in water)")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("Conduit Power")
                        .color(NamedTextColor.GRAY));
                break;
            case "EARTH":
                player.sendMessage(Component.text("Haste 3")
                        .color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("Hero of the Village 1")
                        .color(NamedTextColor.GRAY));
                break;
        }

        // Mace crafted status
        String maceStatus = hasMaceCrafted ? "1/1" : "0/1";
        player.sendMessage(Component.text("**" + elementDisplay + " Mace " + maceStatus + " Crafted**")
                .color(elementColor)
                .decoration(TextDecoration.BOLD, true));

        // Element-specific abilities
        switch (element) {
            case "AIR":
                player.sendMessage(Component.text("Ability 1: Wind Shot - Shoots 1 wind charge")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("Ability 2: Wind Struck - Traps enemies in cobwebs with slow falling")
                        .color(NamedTextColor.WHITE));
                player.sendMessage(Component.text("Passives: Enhanced wind charge pulling effect")
                        .color(NamedTextColor.GRAY));
                break;
            case "FIRE":
                player.sendMessage(Component.text("Ability 1: Obsidian Creation - Converts water to obsidian around enemies")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Ability 2: Meteors - Rain 15 meteors in 7x7 area")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Passives: Ignite enemies on hit, fire immunity when holding mace")
                        .color(NamedTextColor.GRAY));
                break;
            case "WATER":
                player.sendMessage(Component.text("Ability 1: Water Heal - Heals 2 hearts")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("Ability 2: Water Geyser - Launch nearby players up in the air")
                        .color(NamedTextColor.BLUE));
                player.sendMessage(Component.text("Passives: Every hit has a 1% chance to give mining fatigue")
                        .color(NamedTextColor.GRAY));
                break;
            case "EARTH":
                player.sendMessage(Component.text("Ability 1: Buddy Up - Summons protective iron golem")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("Ability 2: Vine Trap - Completely immobilizes enemies for 5 seconds")
                        .color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("Passives: All food acts like golden apples when holding mace, Haste 5 when holding")
                        .color(NamedTextColor.GRAY));
                break;
        }
    }
}