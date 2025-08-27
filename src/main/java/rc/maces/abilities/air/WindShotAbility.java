package rc.maces.abilities.air;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

// WindShot Ability - Shoots 1 wind charge (NO CHAT SPAM)
public class WindShotAbility extends BaseAbility {

    public WindShotAbility(CooldownManager cooldownManager) {
        super("wind_shot", 5, cooldownManager);
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        WindCharge windCharge = player.getWorld().spawn(player.getEyeLocation(), WindCharge.class);
        windCharge.setShooter(player);
        windCharge.setVelocity(player.getEyeLocation().getDirection().multiply(2));

        // REMOVED: Chat message spam - just sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        setCooldown(player);
    }
}