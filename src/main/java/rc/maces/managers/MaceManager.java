package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class MaceManager {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;

    public MaceManager(JavaPlugin plugin, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    public ItemStack createAirMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.displayName(Component.text("Air Mace")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" Mace")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, true)));

        meta.lore(Arrays.asList(
                Component.text("💨 Right-click: Wind Shot (5s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 F key: Air Burst (10s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 Grants immunity to fall damage")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 Strong hits apply slow falling")
                        .color(NamedTextColor.DARK_GRAY)
        ));

        mace.setItemMeta(meta);
        return mace;
    }

    public ItemStack createFireMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.displayName(Component.text("Fire Mace")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" Mace")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true)));

        meta.lore(Arrays.asList(
                Component.text("🔥 Right-click: Water to Lava (10s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🔥 F key: Meteor Shower (25s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🔥 Passive: Ignite enemies on hit")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🔥 Passive: Fire immunity")
                        .color(NamedTextColor.DARK_GRAY)
        ));

        mace.setItemMeta(meta);
        return mace;
    }

    public ItemStack createWaterMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.displayName(Component.text("Water Mace")
                .color(NamedTextColor.DARK_BLUE)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" Mace")
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.BOLD, true)));

        meta.lore(Arrays.asList(
                Component.text("🌊 Right-click: Self Heal +2❤ (10s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌊 F key: Water Geyser (20s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌊 Passive: Conduit Power & Dolphin's Grace")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌊 Passive: Enemies within 4 blocks drown")
                        .color(NamedTextColor.DARK_GRAY)
        ));

        mace.setItemMeta(meta);
        return mace;
    }

    public ItemStack createEarthMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.displayName(Component.text("Earth Mace")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" Mace")
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.BOLD, true)));

        meta.lore(Arrays.asList(
                Component.text("🌍 Right-click: Stone Wall (15s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌍 F key: Earthquake (30s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌍 Passive: Stone Skin effect")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌍 Passive: Immunity to suffocation")
                        .color(NamedTextColor.DARK_GRAY)
        ));

        mace.setItemMeta(meta);
        return mace;
    }

    public boolean isAirMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;
        return meta.displayName().toString().contains("Air Mace");
    }

    public boolean isFireMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;
        return meta.displayName().toString().contains("Fire Mace");
    }

    public boolean isWaterMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;
        return meta.displayName().toString().contains("Water Mace");
    }

    public boolean isEarthMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;
        return meta.displayName().toString().contains("Earth Mace");
    }

    public boolean isCustomMace(ItemStack item) {
        return isAirMace(item) || isFireMace(item) || isWaterMace(item) || isEarthMace(item);
    }

    public void giveAirMace(Player player) {
        player.getInventory().addItem(createAirMace());
    }

    public void giveFireMace(Player player) {
        player.getInventory().addItem(createFireMace());
    }

    public void giveWaterMace(Player player) {
        player.getInventory().addItem(createWaterMace());
    }

    public void giveEarthMace(Player player) {
        player.getInventory().addItem(createEarthMace());
    }

    public String getActionBarStatus(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!isCustomMace(mainHand)) return "";

        UUID playerId = player.getUniqueId();
        StringBuilder sb = new StringBuilder();

        if (isAirMace(mainHand)) {
            sb.append("§f§lAir §7§lMace §8| ");

            // Wind Shot status
            if (cooldownManager.isOnCooldown(playerId, "wind_shot")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "wind_shot") / 1000;
                sb.append("§c§fWind Shot: ").append(remaining).append("s");
            } else {
                sb.append("§a§fWind Shot: Ready");
            }

            sb.append(" §8| ");

            // Air Burst status
            if (cooldownManager.isOnCooldown(playerId, "air_burst")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "air_burst") / 1000;
                sb.append("§c§7Air Burst: ").append(remaining).append("s");
            } else {
                sb.append("§a§7Air Burst: Ready");
            }
        } else if (isFireMace(mainHand)) {
            sb.append("§c§lFire §6§lMace §8| ");

            // Water to Lava status
            if (cooldownManager.isOnCooldown(playerId, "water_to_lava")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "water_to_lava") / 1000;
                sb.append("§c§cWater to Lava: ").append(remaining).append("s");
            } else {
                sb.append("§a§cWater to Lava: Ready");
            }

            sb.append(" §8| ");

            // Meteor Shower status
            if (cooldownManager.isOnCooldown(playerId, "meteor_shower")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "meteor_shower") / 1000;
                sb.append("§c§6Meteor Shower: ").append(remaining).append("s");
            } else {
                sb.append("§a§6Meteor Shower: Ready");
            }
        } else if (isWaterMace(mainHand)) {
            sb.append("§1§lWater §9§lMace §8| ");

            // Self Heal status
            if (cooldownManager.isOnCooldown(playerId, "self_heal")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "self_heal") / 1000;
                sb.append("§c§9Self Heal: ").append(remaining).append("s");
            } else {
                sb.append("§a§9Self Heal: Ready");
            }

            sb.append(" §8| ");

            // Water Geyser status
            if (cooldownManager.isOnCooldown(playerId, "water_geyser")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "water_geyser") / 1000;
                sb.append("§c§1Water Geyser: ").append(remaining).append("s");
            } else {
                sb.append("§a§1Water Geyser: Ready");
            }
        } else if (isEarthMace(mainHand)) {
            sb.append("§2§lEarth §a§lMace §8| ");

            // Stone Wall status
            if (cooldownManager.isOnCooldown(playerId, "stone_wall")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "stone_wall") / 1000;
                sb.append("§c§2Stone Wall: ").append(remaining).append("s");
            } else {
                sb.append("§a§2Stone Wall: Ready");
            }

            sb.append(" §8| ");

            // Earthquake status
            if (cooldownManager.isOnCooldown(playerId, "earthquake")) {
                long remaining = cooldownManager.getRemainingCooldown(playerId, "earthquake") / 1000;
                sb.append("§c§aEarthquake: ").append(remaining).append("s");
            } else {
                sb.append("§a§aEarthquake: Ready");
            }
        }

        return sb.toString();
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}