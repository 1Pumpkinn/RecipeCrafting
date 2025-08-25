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

public class CraftedMacesCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final MaceManager maceManager;

    public CraftedMacesCommand(ElementManager elementManager, MaceManager maceManager) {
        this.elementManager = elementManager;
        this.maceManager = maceManager;
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

        // Count maces in inventory
        int airMaces = countMacesInInventory(player, "air");
        int fireMaces = countMacesInInventory(player, "fire");
        int waterMaces = countMacesInInventory(player, "water");
        int earthMaces = countMacesInInventory(player, "earth");

        // Create the crafted maces display
        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("         CRAFTED MACES")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Your Element: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(elementManager.getElementDisplayName(playerElement))
                        .color(elementManager.getElementColor(playerElement))
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .appendNewline()
                .append(createMaceStatusLine("💨 Air", airMaces, "AIR".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🔥 Fire", fireMaces, "FIRE".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🌊 Water", waterMaces, "WATER".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🌍 Earth", earthMaces, "EARTH".equals(playerElement)))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("You can only craft maces of your element!")
                        .color(NamedTextColor.GRAY));

        player.sendMessage(message);
        return true;
    }

    private Component createMaceStatusLine(String maceType, int count, boolean canCraft) {
        Component line = Component.text(maceType + ": ")
                .color(NamedTextColor.WHITE);

        if (canCraft) {
            // This is their element mace
            line = line.append(Component.text(count + "/∞")
                            .color(NamedTextColor.GREEN)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" ✓")
                            .color(NamedTextColor.GREEN));
        } else {
            // Not their element
            line = line.append(Component.text(count + "/1")
                            .color(NamedTextColor.RED))
                    .append(Component.text(" ✗")
                            .color(NamedTextColor.RED));
        }

        return line;
    }

    private int countMacesInInventory(Player player, String maceType) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            switch (maceType.toLowerCase()) {
                case "air":
                    if (maceManager.isAirMace(item)) count += item.getAmount();
                    break;
                case "fire":
                    if (maceManager.isFireMace(item)) count += item.getAmount();
                    break;
                case "water":
                    if (maceManager.isWaterMace(item)) count += item.getAmount();
                    break;
                case "earth":
                    if (maceManager.isEarthMace(item)) count += item.getAmount();
                    break;
            }
        }
        return count;
    }
}