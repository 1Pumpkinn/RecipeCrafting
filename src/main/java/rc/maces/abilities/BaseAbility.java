package rc.maces.abilities;

import org.bukkit.entity.Player;
import rc.maces.managers.CooldownManager;

public abstract class BaseAbility {

    protected final String abilityKey;
    protected final int cooldownSeconds;
    protected final CooldownManager cooldownManager;

    public BaseAbility(String abilityKey, int cooldownSeconds, CooldownManager cooldownManager) {
        this.abilityKey = abilityKey;
        this.cooldownSeconds = cooldownSeconds;
        this.cooldownManager = cooldownManager;
    }

    public boolean canUse(Player player) {
        return !cooldownManager.isOnCooldown(player.getUniqueId(), abilityKey);
    }

    public void setCooldown(Player player) {
        cooldownManager.setCooldown(player.getUniqueId(), abilityKey, cooldownSeconds * 1000L);
    }

    public long getRemainingCooldown(Player player) {
        return cooldownManager.getRemainingCooldown(player.getUniqueId(), abilityKey);
    }

    public abstract void execute(Player player);

    public String getAbilityKey() {
        return abilityKey;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}