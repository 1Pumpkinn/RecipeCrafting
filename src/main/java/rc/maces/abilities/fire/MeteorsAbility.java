package rc.maces.abilities.fire;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;
import java.util.Random;

// Meteors Ability - Drops 10 meteors in 5x5 radius
public class MeteorsAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public MeteorsAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("meteors", 25, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();

        player.sendMessage(Component.text("☄️ METEORS! Raining destruction!")
                .color(NamedTextColor.RED));
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.4f);

        new BukkitRunnable() {
            int meteorsLaunched = 0;

            @Override
            public void run() {
                if (meteorsLaunched >= 10) {
                    cancel();
                    return;
                }

                // Random location in 5x5 radius
                int randomX = random.nextInt(5) - 2;
                int randomZ = random.nextInt(5) - 2;

                Location targetLoc = center.clone().add(randomX, 0, randomZ);
                Location meteorLoc = targetLoc.clone().add(0, 15, 0);

                // Create warning effects
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 10);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 5);

                // Launch meteor
                LargeFireball meteor = meteorLoc.getWorld().spawn(meteorLoc, LargeFireball.class);
                meteor.setShooter(player);
                meteor.setDirection(new Vector(0, -1, 0));
                meteor.setVelocity(new Vector(0, -2, 0));

                // Schedule impact
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        meteorImpact(targetLoc, player);
                    }
                }.runTaskLater(plugin, 15L);

                meteorsLaunched++;
            }
        }.runTaskTimer(plugin, 0L, 3L);

        setCooldown(player);
    }

    private void meteorImpact(Location targetLoc, Player caster) {
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 20);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 15);

        // Deal 2 hearts (4 damage) true damage to nearby living entities
        Collection<Entity> nearby = targetLoc.getWorld().getNearbyEntities(targetLoc, 2, 2, 2);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != caster) {
                LivingEntity living = (LivingEntity) entity;

                // True damage - bypass armor/resistance
                double newHealth = Math.max(0, living.getHealth() - 4.0);
                living.setHealth(newHealth);

                entity.setFireTicks(100);

                // Send message only to players
                if (entity instanceof Player) {
                    ((Player) entity).sendMessage(Component.text("☄️ STRUCK BY METEOR!")
                            .color(NamedTextColor.RED));
                }
            }
        }
    }
}