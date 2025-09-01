package rc.maces.abilities.water;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;

// Water Heal Ability - OPTIONAL: Can remove message entirely if desired
public class WaterHealAbility extends BaseAbility {

    public WaterHealAbility(CooldownManager cooldownManager) {
        super("water_heal", 10, cooldownManager);
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        // Heal 2 hearts (4 health points)
        double newHealth = Math.min(player.getHealth() + 4.0, player.getMaxHealth());
        player.setHealth(newHealth);

        // SIMPLIFIED: Keep this message as it's useful feedback for self-healing
        // Comment out the line below if you want NO chat messages at all
        player.sendMessage(Component.text("🌊 Healed 2 hearts!")
                .color(NamedTextColor.BLUE));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5);

        setCooldown(player);
    }
}