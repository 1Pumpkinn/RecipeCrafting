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

        // Get global crafting status
        Map<String, Boolean> globalStatus = craftingListener.getGlobalMaceCraftedStatus();
        Map<String, String> globalCrafters = craftingListener.getGlobalMaceCrafters();

        // Create the maces status display
        Component message = Component.text("SERVER MACE STATUS:")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Air Mace", "AIR", globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Fire Mace", "FIRE", globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Water Mace", "WATER", globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Earth Mace", "EARTH", globalStatus, globalCrafters, player.getName()));

        player.sendMessage(message);
        return true;
    }

    private Component createGlobalMaceStatusLine(String maceType, String maceKey,
                                                 Map<String, Boolean> globalStatus, Map<String, String> globalCrafters,
                                                 String currentPlayerName) {
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

        Component maceNameComponent = Component.text(maceType + " | ")
                .color(maceColor);

        boolean isGlobalCrafted = globalStatus.getOrDefault(maceKey, false);

        if (isGlobalCrafted) {
            // Show only 1/1
            return maceNameComponent
                    .append(Component.text("1/1")
                            .color(NamedTextColor.GREEN)
                            .decoration(TextDecoration.BOLD, true));
        } else {
            // Show only 0/1
            return maceNameComponent
                    .append(Component.text("0/1")
                            .color(NamedTextColor.GREEN));
        }
    }

}