package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.abilities.AbilityManager;

import java.util.Arrays;
import java.util.UUID;

public class MaceManager {

    private final JavaPlugin plugin;
    private final CooldownManager cooldownManager;
    private final AbilityManager abilityManager;

    public MaceManager(JavaPlugin plugin, CooldownManager cooldownManager, TrustManager trustManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.abilityManager = new AbilityManager(plugin, cooldownManager, this, trustManager);
    }

    public ItemStack createAirMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        meta.displayName(Component.text("Air Mace")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("💨 Right-click: Wind Shot (5s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("💨 F key: Wind Struck (25s cooldown)")
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
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("🔥 Right-click: Obsidian Creation (30s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🔥 F key: Meteors (25s cooldown)")
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
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("🌊 Right-click: Water Heal +2❤ (10s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌊 F key: Water Geyser (30s cooldown)")
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
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("🌍 Right-click: Buddy Up (15s cooldown)")
                        .color(NamedTextColor.DARK_GRAY),
                Component.text("🌍 F key: Vine Trap (25s cooldown)")
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
            appendAbilityStatus(sb, player, AbilityManager.WIND_SHOT, "Wind Shot", "§f");
            sb.append(" §8| ");
            appendAbilityStatus(sb, player, AbilityManager.WIND_STRUCK, "Wind Struck", "§7");
        } else if (isFireMace(mainHand)) {
            sb.append("§c§lFire §6§lMace §8| ");
            appendAbilityStatus(sb, player, AbilityManager.OBSIDIAN_CREATION, "Obsidian Creation", "§6");
            sb.append(" §8| ");
            appendAbilityStatus(sb, player, AbilityManager.METEORS, "Meteors", "§6");
        } else if (isWaterMace(mainHand)) {
            sb.append("§1§lWater §9§lMace §8| ");
            appendAbilityStatus(sb, player, AbilityManager.WATER_HEAL, "Water Heal", "§9");
            sb.append(" §8| ");
            appendAbilityStatus(sb, player, AbilityManager.WATER_GEYSER, "Water Geyser", "§1");
        } else if (isEarthMace(mainHand)) {
            sb.append("§2§lEarth §a§lMace §8| ");
            appendAbilityStatus(sb, player, AbilityManager.BUDDY_UP, "Buddy Up", "§2");
            sb.append(" §8| ");
            appendAbilityStatus(sb, player, AbilityManager.VINE_TRAP, "Vine Trap", "§a");
        }

        return sb.toString();
    }

    private void appendAbilityStatus(StringBuilder sb, Player player, String abilityKey, String abilityName, String color) {
        if (abilityManager.canUseAbility(player, abilityKey)) {
            sb.append("§a").append(color).append(abilityName).append(": Ready");
        } else {
            long remaining = abilityManager.getRemainingCooldown(player, abilityKey) / 1000;
            sb.append("§c").append(color).append(abilityName).append(": ").append(remaining).append("s");
        }
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}