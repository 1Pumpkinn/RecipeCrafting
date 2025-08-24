package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;

// Tornado Ability - Pulls all players in 5x5 radius towards you
public class TornadoAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public TornadoAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("tornado", 20, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    cancel();
                    return;
                }

                // Pull players towards center every 5 ticks
                if (ticks % 5 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5);
                    for (Entity entity : nearby) {
                        if (entity instanceof Player && entity != player) {
                            Player target = (Player) entity;

                            // Calculate direction towards center
                            Vector direction = center.toVector()
                                    .subtract(target.getLocation().toVector())
                                    .normalize()
                                    .multiply(1.5);

                            target.setVelocity(direction);

                            target.sendMessage(Component.text("🌪️ Pulled by tornado force!")
                                    .color(NamedTextColor.GOLD));
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("🌪️ TORNADO! Pulling enemies towards you!")
                .color(NamedTextColor.GOLD));
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.6f);

        setCooldown(player);
    }
}