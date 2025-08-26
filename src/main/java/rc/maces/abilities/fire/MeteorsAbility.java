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
import rc.maces.managers.TrustManager;

import java.util.Collection;
import java.util.Random;

// NERFED Meteors Ability - Reduced damage to 2 hearts (4 damage) to work better against Protection 4 players
public class MeteorsAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;
    private final Random random = new Random();

    public MeteorsAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("meteors", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
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
                if (meteorsLaunched >= 15) {
                    cancel();
                    return;
                }

                // Random location in 8x8 radius (4 blocks each direction)
                int randomX = random.nextInt(9) - 4; // -4 to +4
                int randomZ = random.nextInt(9) - 4; // -4 to +4

                Location targetLoc = center.clone().add(randomX, 0, randomZ);
                Location meteorLoc = targetLoc.clone().add(0, 20, 0);

                // Create warning effects
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 20);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10);
                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 15);

                // Launch meteor with higher speed
                LargeFireball meteor = meteorLoc.getWorld().spawn(meteorLoc, LargeFireball.class);
                meteor.setShooter(player);
                meteor.setDirection(new Vector(0, -1, 0));
                meteor.setVelocity(new Vector(0, -2.5, 0));
                meteor.setYield(0.0f); // Set explosion yield to 0 to prevent block breaking

                // Schedule impact
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        meteorImpact(targetLoc, player);
                    }
                }.runTaskLater(plugin, 12L);

                meteorsLaunched++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        setCooldown(player);
    }

    private void meteorImpact(Location targetLoc, Player caster) {
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.4f);
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 30);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 25);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10);

        // NERFED: Deal 2 hearts (4 damage) true damage to ALL nearby living entities in 8 block range (except allies)
        Collection<Entity> nearby = targetLoc.getWorld().getNearbyEntities(targetLoc, 4, 4, 4); // 8 block range
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != caster) {
                LivingEntity living = (LivingEntity) entity;

                // Check trust system - don't damage trusted players
                if (living instanceof Player && trustManager.isTrusted(caster, (Player) living)) {
                    continue;
                }

                // NERFED: Reduced from 6 to 4 damage (2 hearts instead of 3)
                double newHealth = Math.max(0, living.getHealth() - 4.0);
                living.setHealth(newHealth);

                // Set fire to ALL living entities
                entity.setFireTicks(150);
            }
        }
    }
}