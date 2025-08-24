package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {

    private final MaceManager maceManager;
    private final Plugin plugin;

    public ActionBarManager(MaceManager maceManager, Plugin plugin) {
        this.maceManager = maceManager;
        this.plugin = plugin;
        startActionBarTask();
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();

                    if (mainHand.getType() == Material.MACE && maceManager.isAirMace(mainHand)) {
                        displayAirMaceActionBar(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Every tick like in Skript
    }

    private void displayAirMaceActionBar(Player player) {
        String ability1Status = getAbilityStatus(player, "wind_shot", 5000);
        String ability2Status = getAbilityStatus(player, "air_burst", 10000);

        // Create action bar message matching Skript format
        Component actionBar = Component.text("Air Mace")
                .color(NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .append(Component.text(" | ")
                        .color(NamedTextColor.DARK_GRAY))
                .append(parseAbilityComponent(ability1Status, "Wind Shot"))
                .append(Component.text(" | ")
                        .color(NamedTextColor.DARK_GRAY))
                .append(parseAbilityComponent(ability2Status, "Air Burst"));

        player.sendActionBar(actionBar);
    }

    private String getAbilityStatus(Player player, String abilityKey, long cooldownMs) {
        if (maceManager.getCooldownManager().isOnCooldown(player.getUniqueId(), abilityKey)) {
            long remainingMs = maceManager.getCooldownManager().getRemainingCooldown(player.getUniqueId(), abilityKey);
            int seconds = (int) Math.ceil(remainingMs / 1000.0);
            return "COOLDOWN:" + seconds;
        }
        return "READY";
    }

    private Component parseAbilityComponent(String status, String abilityName) {
        if (status.equals("READY")) {
            return Component.text(abilityName + ": Ready")
                    .color(NamedTextColor.GREEN);
        } else if (status.startsWith("COOLDOWN:")) {
            String seconds = status.substring("COOLDOWN:".length());
            return Component.text(abilityName + ": " + seconds + "s")
                    .color(NamedTextColor.RED);
        }
        return Component.text(abilityName + ": Unknown")
                .color(NamedTextColor.GRAY);
    }
}