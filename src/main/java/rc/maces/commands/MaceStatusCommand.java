package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import rc.maces.listeners.CraftingListener;

import java.util.Map;

public class MaceStatusCommand implements CommandExecutor {

    private final CraftingListener craftingListener;

    public MaceStatusCommand(CraftingListener craftingListener) {
        this.craftingListener = craftingListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Get global crafting status
        Map<String, Boolean> globalStatus = craftingListener.getGlobalMaceCraftedStatus();
        Map<String, String> globalCrafters = craftingListener.getGlobalMaceCrafters();

        // Create the status display
        Component message = Component.text("=== SERVER MACE STATUS ===")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .appendNewline()
                .appendNewline()
                .append(createMaceStatusLine("Air Mace", "AIR", NamedTextColor.WHITE, globalStatus, globalCrafters))
                .appendNewline()
                .append(createMaceStatusLine("Fire Mace", "FIRE", NamedTextColor.RED, globalStatus, globalCrafters))
                .appendNewline()
                .append(createMaceStatusLine("Water Mace", "WATER", NamedTextColor.BLUE, globalStatus, globalCrafters))
                .appendNewline()
                .append(createMaceStatusLine("Earth Mace", "EARTH", NamedTextColor.GREEN, globalStatus, globalCrafters))
                .appendNewline()
                .appendNewline()
                .append(Component.text("⚠️ Only ONE of each mace type can exist on the server!")
                        .color(NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("Use /craftedmaces to see detailed status")
                        .color(NamedTextColor.GRAY));

        sender.sendMessage(message);
        return true;
    }

    private Component createMaceStatusLine(String maceName, String maceKey, NamedTextColor color,
                                           Map<String, Boolean> globalStatus, Map<String, String> globalCrafters) {
        Component maceNameComponent = Component.text("🔹 " + maceName + ": ")
                .color(color)
                .decoration(TextDecoration.BOLD, true);

        boolean isGlobalCrafted = globalStatus.getOrDefault(maceKey, false);

        if (isGlobalCrafted) {
            String crafter = globalCrafters.getOrDefault(maceKey, "Unknown");
            return maceNameComponent
                    .append(Component.text("1/1 - CRAFTED by " + crafter)
                            .color(NamedTextColor.GRAY))
                    .append(Component.text(" ❌")
                            .color(NamedTextColor.RED));
        } else {
            return maceNameComponent
                    .append(Component.text("0/1 - AVAILABLE")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text(" ✅")
                            .color(NamedTextColor.GREEN));
        }
    }
}