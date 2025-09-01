package rc.maces.abilities.fire;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
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

// Meteors Ability - NERFED: Now does exactly 1 heart (2 damage) per meteor hit regardless of armor
public class MeteorsAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;
    private final Random random = new Random();

    public MeteorsAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("meteors", 60, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();

        // SIMPLIFIED: Only one message at ability start
        player.sendMessage(Component.text("☄️ METEORS! Raining destruction!")
                .color(NamedTextColor.RED));
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.4f);

        new BukkitRunnable() {
            int meteorsLaunched = 0;

            @Override
            public void run() {
                if (meteorsLaunched >= 6) {
                    cancel();
                    return;
                }

                // Improved random location in 12x12 radius (6 blocks each direction) for better range
                int randomX = random.nextInt(13) - 6; // -6 to +6
                int randomZ = random.nextInt(13) - 6; // -6 to +6

                Location targetLoc = center.clone().add(randomX, 0, randomZ);
                Location meteorLoc = targetLoc.clone().add(0, 25, 0); // Higher spawn for more dramatic effect

                // Enhanced warning effects
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 25);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 15);
                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 20);
                targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc, 10); // Add crit particles for warning

                // Launch meteor with consistent speed
                LargeFireball meteor = meteorLoc.getWorld().spawn(meteorLoc, LargeFireball.class);
                meteor.setShooter(player);
                meteor.setDirection(new Vector(0, -1, 0));
                meteor.setVelocity(new Vector(0, -2.0, 0)); // Slightly slower for more dramatic effect
                meteor.setYield(0.0f); // No block breaking

                // Schedule impact (adjusted for new height)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        meteorImpact(targetLoc, player);
                    }
                }.runTaskLater(plugin, 15L); // Slightly longer delay for higher spawn

                meteorsLaunched++;
            }
        }.runTaskTimer(plugin, 0L, 3L); // Slightly longer delay between meteors

        setCooldown(player);
    }

    private void meteorImpact(Location targetLoc, Player caster) {
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.4f);
        // Enhanced explosion effects
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 35);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 30);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 15);
        targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 25);

        // Deal flat 1 heart (2 damage) to ALL nearby living entities in 5 block range
        Collection<Entity> nearby = targetLoc.getWorld().getNearbyEntities(targetLoc, 5, 5, 5);
        int affectedCount = 0; // Track affected entities for summary message

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != caster) {
                LivingEntity living = (LivingEntity) entity;

                // TRUST SYSTEM CHECK - Skip trusted players (NO SPAM MESSAGE)
                if (living instanceof Player targetPlayer) {
                    if (trustManager.isTrusted(caster, targetPlayer)) {
                        // REMOVED: No spam message to trusted players
                        continue;
                    }

                    // Skip creative/spectator players
                    if (targetPlayer.getGameMode() == GameMode.CREATIVE || targetPlayer.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                }

                // SIMPLIFIED DAMAGE SYSTEM - Always 1 heart (2 damage) regardless of armor
                double damage = 2.0; // Exactly 1 heart

                // Apply damage safely (never kill instantly, always leave at least 1 HP)
                double currentHealth = living.getHealth();
                double newHealth = Math.max(1.0, currentHealth - damage); // Always leave 1 HP minimum
                living.setHealth(newHealth);

                // Apply fire effect
                entity.setFireTicks(120); // 6 seconds of fire

                // Add knockback effect
                Vector knockback = entity.getLocation().toVector().subtract(targetLoc.toVector());
                if (knockback.lengthSquared() > 0) {
                    knockback = knockback.normalize().multiply(1.2);
                    knockback.setY(Math.max(0.5, knockback.getY())); // Ensure upward knockback
                    entity.setVelocity(knockback);
                }

                affectedCount++; // Count this entity as affected
            }
        }
    }
}