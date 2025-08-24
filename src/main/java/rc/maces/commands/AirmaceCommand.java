package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AirmaceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("airmace.give")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /airmace <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' is not online!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Create Air Mace item
        ItemStack airMace = new ItemStack(Material.MACE);
        ItemMeta meta = airMace.getItemMeta();

        // Set name and lore to match Skript version
        meta.displayName(Component.text("Air Mace")
                .color(NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .append(Component.text(" Mace")
                        .color(NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)));

        meta.lore(List.of(
                Component.text("💨 Right-click: Wind Shot (5s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 F key: Air Burst (10s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 Grants immunity to fall damage")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 Strong hits apply slow falling")
                        .color(NamedTextColor.DARK_GRAY)
        ));

        airMace.setItemMeta(meta);

        // Give item to target
        target.getInventory().addItem(airMace);

        // Send messages
        sender.sendMessage(Component.text("You have given " + target.getName() + " the ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Air Mace")
                        .color(NamedTextColor.WHITE)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true))
                .append(Component.text("!")
                        .color(NamedTextColor.LIGHT_PURPLE)));

        target.sendMessage(Component.text("You have been given the ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("Air Mace")
                        .color(NamedTextColor.WHITE)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true))
                .append(Component.text("!")
                        .color(NamedTextColor.LIGHT_PURPLE)));

        return true;
    }
}