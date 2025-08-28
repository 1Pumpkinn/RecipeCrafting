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

        // Get personal crafted counts from the crafting listener
        Map<String, Integer> personalCounts = craftingListener.getPlayerMaceCounts(player.getUniqueId());

        // NEW: Get global crafting status
        Map<String, Boolean> globalStatus = craftingListener.getGlobalMaceCraftedStatus();
        Map<String, String> globalCrafters = craftingListener.getGlobalMaceCrafters();

        // Get personal counts
        int airCrafted = personalCounts.getOrDefault("AIR", 0);
        int fireCrafted = personalCounts.getOrDefault("FIRE", 0);
        int waterCrafted = personalCounts.getOrDefault("WATER", 0);
        int earthCrafted = personalCounts.getOrDefault("EARTH", 0);

        // Create the maces status display
        Component message = Component.text("SERVER MACE STATUS:")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Air Mace", "AIR", airCrafted, globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Fire Mace", "FIRE", fireCrafted, globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Water Mace", "WATER", waterCrafted, globalStatus, globalCrafters, player.getName()))
                .appendNewline()
                .append(Component.text("• ").color(NamedTextColor.WHITE))
                .append(createGlobalMaceStatusLine("Earth Mace", "EARTH", earthCrafted, globalStatus, globalCrafters, player.getName()));

        player.sendMessage(message);
        return true;
    }

    private Component createGlobalMaceStatusLine(String maceType, String maceKey, int personalCount,
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
            String crafter = globalCrafters.getOrDefault(maceKey, "Unknown");
            if (crafter.equals(currentPlayerName)) {
                // Player viewing is the one who crafted it
                return maceNameComponent
                        .append(Component.text("CRAFTED BY YOU ⭐")
                                .color(NamedTextColor.GOLD)
                                .decoration(TextDecoration.BOLD, true));
            } else {
                // Someone else crafted it
                return maceNameComponent
                        .append(Component.text("Crafted by " + crafter)
                                .color(NamedTextColor.GRAY));
            }
        } else {
            // Not yet crafted
            return maceNameComponent
                    .append(Component.text("Available to craft")
                            .color(NamedTextColor.GREEN));
        }
    }
}