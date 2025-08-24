package rc.maces.abilities.air;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;

// WindShot Ability - Shoots 1 wind charge
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

        player.sendMessage(Component.text("💨 Wind Shot launched!")
                .color(NamedTextColor.WHITE));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        setCooldown(player);
    }
}