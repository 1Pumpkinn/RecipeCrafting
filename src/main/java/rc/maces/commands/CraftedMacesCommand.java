package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rc.maces.listeners.CraftingListener;
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

import java.util.Map;

public class CraftedMacesCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final MaceManager maceManager;
    private final CraftingListener craftingListener;

    public CraftedMacesCommand(ElementManager elementManager, MaceManager maceManager, CraftingListener craftingListener) {
        this.elementManager = elementManager;
        this.maceManager = maceManager;
        this.craftingListener = craftingListener;
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

        // Get crafted counts from the crafting listener
        Map<String, Integer> craftedCounts = craftingListener.getPlayerMaceCounts(player.getUniqueId());

        // Get crafted counts (how many they've actually crafted)
        int airCrafted = craftedCounts.getOrDefault("AIR", 0);
        int fireCrafted = craftedCounts.getOrDefault("FIRE", 0);
        int waterCrafted = craftedCounts.getOrDefault("WATER", 0);
        int earthCrafted = craftedCounts.getOrDefault("EARTH", 0);

        // Create the crafted maces display
        Component message = Component.text("CRAFTED MACES:")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createMaceStatusLine("Air Mace", airCrafted))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createMaceStatusLine("Fire Mace", fireCrafted))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createMaceStatusLine("Water Mace", waterCrafted))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createMaceStatusLine("Earth Mace", earthCrafted));

        player.sendMessage(message);
        return true;
    }

    private Component createMaceStatusLine(String maceType, int craftedCount) {
        // Color code the mace type based on element
        NamedTextColor maceColor;
        if (maceType.contains("Air")) {
            maceColor = NamedTextColor.WHITE;
        } else if (maceType.contains("Fire")) {
            maceColor = NamedTextColor.RED;
        } else if (maceType.contains("Water")) {
            maceColor = NamedTextColor.BLUE;
        } else if (maceType.contains("Earth")) {
            maceColor = NamedTextColor.DARK_GREEN;
        } else {
            maceColor = NamedTextColor.WHITE;
        }

        return Component.text(maceType + " | ")
                .color(maceColor)
                .append(Component.text("Crafted: " + craftedCount + "/1")
                        .color(NamedTextColor.GREEN));
    }
}