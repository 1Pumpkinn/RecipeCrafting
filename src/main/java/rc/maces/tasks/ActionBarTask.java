package rc.maces.tasks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.AbilityManager;
import rc.maces.managers.MaceManager;

import java.util.UUID;

public class ActionBarTask extends BukkitRunnable {

    private final MaceManager maceManager;

    public ActionBarTask(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();

            if (mainHand.getType() == Material.MACE && maceManager.isCustomMace(mainHand)) {
                displayMaceActionBar(player, mainHand);
            }
        }
    }

    private void displayMaceActionBar(Player player, ItemStack mace) {
        UUID playerId = player.getUniqueId();
        Component actionBar = null;

        if (maceManager.isAirMace(mace)) {
            actionBar = createActionBar("Air", NamedTextColor.WHITE, NamedTextColor.GRAY,
                    "Wind Shot", getAbilityStatus(playerId, AbilityManager.WIND_SHOT),
                    "Wind Struck", getAbilityStatus(playerId, AbilityManager.WIND_STRUCK));
        } else if (maceManager.isFireMace(mace)) {
            actionBar = createActionBar("Fire", NamedTextColor.RED, NamedTextColor.GOLD,
                    "Obsidian Creation", getAbilityStatus(playerId, AbilityManager.OBSIDIAN_CREATION),
                    "Meteors", getAbilityStatus(playerId, AbilityManager.METEORS));
        } else if (maceManager.isWaterMace(mace)) {
            actionBar = createActionBar("Water", NamedTextColor.DARK_BLUE, NamedTextColor.BLUE,
                    "Water Heal", getAbilityStatus(playerId, AbilityManager.WATER_HEAL),
                    "Water Geyser", getAbilityStatus(playerId, AbilityManager.WATER_GEYSER));
        } else if (maceManager.isEarthMace(mace)) {
            actionBar = createActionBar("Earth", NamedTextColor.GREEN, NamedTextColor.DARK_GREEN,
                    "Buddy Up", getAbilityStatus(playerId, AbilityManager.BUDDY_UP),
                    "Vine Trap", getAbilityStatus(playerId, AbilityManager.VINE_TRAP)); // CHANGED: Display name to "Vine Trap"
        }

        if (actionBar != null) {
            player.sendActionBar(actionBar);
        }
    }

    private Component createActionBar(String maceType, NamedTextColor primaryColor, NamedTextColor secondaryColor,
                                      String ability1Name, String ability1Status,
                                      String ability2Name, String ability2Status) {
        return Component.text(maceType + " Mace")
                .color(primaryColor)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" | ")
                        .color(NamedTextColor.DARK_GRAY))
                .append(parseAbilityComponent(ability1Status, ability1Name))
                .append(Component.text(" | ")
                        .color(NamedTextColor.DARK_GRAY))
                .append(parseAbilityComponent(ability2Status, ability2Name));
    }

    private Component createSingleAbilityActionBar(String maceType, NamedTextColor primaryColor,
                                                   String abilityName, String abilityStatus) {
        return Component.text(maceType + " Mace")
                .color(primaryColor)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(" | ")
                        .color(NamedTextColor.DARK_GRAY))
                .append(parseAbilityComponent(abilityStatus, abilityName));
    }

    private String getAbilityStatus(UUID playerId, String abilityKey) {
        if (maceManager.getCooldownManager().isOnCooldown(playerId, abilityKey)) {
            long remainingMs = maceManager.getCooldownManager().getRemainingCooldown(playerId, abilityKey);
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