package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

        // Count maces currently in inventory
        int airInInventory = countMacesInInventory(player, "air");
        int fireInInventory = countMacesInInventory(player, "fire");
        int waterInInventory = countMacesInInventory(player, "water");
        int earthInInventory = countMacesInInventory(player, "earth");

        // Get crafted counts (how many they've actually crafted)
        int airCrafted = craftedCounts.getOrDefault("AIR", 0);
        int fireCrafted = craftedCounts.getOrDefault("FIRE", 0);
        int waterCrafted = craftedCounts.getOrDefault("WATER", 0);
        int earthCrafted = craftedCounts.getOrDefault("EARTH", 0);

        // Create the crafted maces display
        Component message = Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("         MACE STATUS")
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
                .append(Component.text("CRAFTING STATUS:")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true))
                .appendNewline()
                .append(createMaceStatusLine("💨 Air", airCrafted, airInInventory, "AIR".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🔥 Fire", fireCrafted, fireInInventory, "FIRE".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🌊 Water", waterCrafted, waterInInventory, "WATER".equals(playerElement)))
                .appendNewline()
                .append(createMaceStatusLine("🌍 Earth", earthCrafted, earthInInventory, "EARTH".equals(playerElement)))
                .appendNewline()
                .append(Component.text("═══════════════════════════════════")
                        .color(NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Crafted: How many you've made (1/1 max)")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("In Inventory: How many you currently have")
                        .color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("✅ = Can craft  |  ❌ = Already crafted  |  🚫 = Wrong element")
                        .color(NamedTextColor.GRAY));

        player.sendMessage(message);
        return true;
    }

    private Component createMaceStatusLine(String maceType, int craftedCount, int inventoryCount, boolean isPlayerElement) {
        Component line = Component.text(maceType + ": ")
                .color(NamedTextColor.WHITE);

        // Show crafted status
        if (isPlayerElement) {
            // This is their element - they can craft it
            if (craftedCount >= 1) {
                // Already crafted
                line = line.append(Component.text("Crafted: 1/1")
                                .color(NamedTextColor.RED))
                        .append(Component.text(" | In Inventory: " + inventoryCount)
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(" ❌")
                                .color(NamedTextColor.RED));
            } else {
                // Can still craft
                line = line.append(Component.text("Crafted: 0/1")
                                .color(NamedTextColor.GREEN))
                        .append(Component.text(" | In Inventory: " + inventoryCount)
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(" ✅")
                                .color(NamedTextColor.GREEN));
            }
        } else {
            // Not their element - they cannot craft it
            line = line.append(Component.text("Crafted: " + craftedCount + "/1")
                            .color(NamedTextColor.GRAY))
                    .append(Component.text(" | In Inventory: " + inventoryCount)
                            .color(NamedTextColor.YELLOW))
                    .append(Component.text(" 🚫")
                            .color(NamedTextColor.DARK_RED));
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