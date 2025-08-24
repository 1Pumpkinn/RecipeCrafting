package rc.maces.abilities;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.abilities.air.WindShotAbility;
import rc.maces.abilities.air.WindStruckAbility;
import rc.maces.abilities.earth.BuddyUpAbility;
import rc.maces.abilities.earth.VinePullAbility;
import rc.maces.abilities.fire.ObsidianCreationAbility;
import rc.maces.abilities.fire.MeteorsAbility;
import rc.maces.abilities.water.WaterGeyserAbility;
import rc.maces.abilities.water.WaterHealAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.MaceManager;

import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private final Map<String, BaseAbility> abilities;
    private final MaceManager maceManager;

    public AbilityManager(JavaPlugin plugin, CooldownManager cooldownManager, MaceManager maceManager) {
        this.abilities = new HashMap<>();
        this.maceManager = maceManager;

        // Register all abilities
        registerAbility(new WindShotAbility(cooldownManager));
        registerAbility(new WindStruckAbility(cooldownManager, plugin));
        registerAbility(new ObsidianCreationAbility(cooldownManager, plugin));
        registerAbility(new MeteorsAbility(cooldownManager, plugin));
        registerAbility(new WaterHealAbility(cooldownManager));
        registerAbility(new WaterGeyserAbility(cooldownManager, plugin));
        registerAbility(new BuddyUpAbility(cooldownManager, plugin));
        registerAbility(new VinePullAbility(cooldownManager, plugin));
    }

    private void registerAbility(BaseAbility ability) {
        abilities.put(ability.getAbilityKey(), ability);
    }

    public void executeAbility(Player player, String abilityKey) {
        BaseAbility ability = abilities.get(abilityKey);
        if (ability != null && ability.canUse(player)) {
            ability.execute(player);
        }
    }

    public BaseAbility getAbility(String abilityKey) {
        return abilities.get(abilityKey);
    }

    public boolean canUseAbility(Player player, String abilityKey) {
        BaseAbility ability = abilities.get(abilityKey);
        return ability != null && ability.canUse(player);
    }

    public long getRemainingCooldown(Player player, String abilityKey) {
        BaseAbility ability = abilities.get(abilityKey);
        return ability != null ? ability.getRemainingCooldown(player) : 0;
    }

    // Ability key constants for easier reference
    public static final String WIND_SHOT = "wind_shot";
    public static final String WIND_STRUCK = "wind_struck";
    public static final String OBSIDIAN_CREATION = "obsidian_creation";
    public static final String METEORS = "meteors";
    public static final String WATER_HEAL = "water_heal";
    public static final String WATER_GEYSER = "water_geyser";
    public static final String BUDDY_UP = "buddy_up";
    public static final String VINE_PULL = "vine_pull";
}