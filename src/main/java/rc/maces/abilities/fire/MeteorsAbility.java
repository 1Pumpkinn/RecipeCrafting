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

// BUFFED Meteors Ability - Drops 15 meteors in 7x7 radius with increased damage
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
                if (meteorsLaunched >= 15) { // BUFFED: Increased from 10 to 15 meteors
                    cancel();
                    return;
                }

                // BUFFED: Random location in 7x7 radius (increased from 5x5)
                int randomX = random.nextInt(7) - 3;
                int randomZ = random.nextInt(7) - 3;

                Location targetLoc = center.clone().add(randomX, 0, randomZ);
                Location meteorLoc = targetLoc.clone().add(0, 20, 0); // BUFFED: Higher drop from 20 blocks

                // Create warning effects - MORE INTENSE
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 20); // Doubled particles
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10); // Doubled particles
                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 15); // Added smoke

                // Launch meteor with higher speed
                LargeFireball meteor = meteorLoc.getWorld().spawn(meteorLoc, LargeFireball.class);
                meteor.setShooter(player);
                meteor.setDirection(new Vector(0, -1, 0));
                meteor.setVelocity(new Vector(0, -2.5, 0)); // BUFFED: Faster fall speed

                // Schedule impact
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        meteorImpact(targetLoc, player);
                    }
                }.runTaskLater(plugin, 12L); // Faster impact due to higher speed

                meteorsLaunched++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // BUFFED: Faster meteor spawning (every 2 ticks instead of 3)

        setCooldown(player);
    }

    private void meteorImpact(Location targetLoc, Player caster) {
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.4f); // Louder sound
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 30); // More particles
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 25); // More flames
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10); // Added lava particles

        // BUFFED: Deal 3 hearts (6 damage) true damage to nearby living entities in larger radius
        Collection<Entity> nearby = targetLoc.getWorld().getNearbyEntities(targetLoc, 3, 3, 3); // Increased radius from 2 to 3
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != caster) {
                LivingEntity living = (LivingEntity) entity;

                // BUFFED: True damage - bypass armor/resistance (increased from 4 to 6 damage)
                double newHealth = Math.max(0, living.getHealth() - 6.0);
                living.setHealth(newHealth);

                // BUFFED: Longer fire duration
                entity.setFireTicks(150); // Increased from 100 to 150 ticks
            }
        }
    }
}