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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.Collection;
import java.util.Random;

// Meteors Ability - variable damage depending on armor worn (REDUCED CHAT SPAM)
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

        // Deal variable damage to ALL nearby living entities in 5 block range (10 block diameter)
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

                // BALANCED DAMAGE SYSTEM - Won't one-shot full iron
                double damage = calculateDamage(living);

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
        }}

    private double calculateDamage(LivingEntity living) {
        // Default damage for mobs
        double damage = 6.0; // 3 hearts

        if (living instanceof Player targetPlayer) {
            ItemStack[] armor = targetPlayer.getInventory().getArmorContents();

            int armorPieces = 0;
            boolean hasIron = false;
            boolean hasDiamond = false;
            boolean hasNetherite = false;

            // Count armor pieces and check materials
            for (ItemStack piece : armor) {
                if (piece != null && !piece.getType().isAir()) {
                    armorPieces++;
                    String material = piece.getType().name();

                    if (material.contains("IRON")) {
                        hasIron = true;
                    } else if (material.contains("DIAMOND")) {
                        hasDiamond = true;
                    } else if (material.contains("NETHERITE")) {
                        hasNetherite = true;
                    }
                }
            }

            // Damage calculation based on armor
            if (armorPieces == 0) {
                damage = 12.0; // 6 hearts (no armor)
            } else if (armorPieces == 4) {
                // Full armor set
                if (hasNetherite) {
                    damage = 4.0; // 2 hearts (netherite is strongest)
                } else if (hasDiamond) {
                    damage = 5.0; // 2.5 hearts (diamond)
                } else if (hasIron) {
                    damage = 7.0; // 3.5 hearts (iron - won't one-shot from full health)
                } else {
                    damage = 8.0; // 4 hearts (leather/chainmail/mixed)
                }
            } else {
                // Partial armor - scale damage based on pieces
                double baseReduction = 0.15 * armorPieces; // 15% reduction per piece
                if (hasNetherite || hasDiamond) {
                    baseReduction += 0.1; // Bonus reduction for high-tier armor
                } else if (hasIron) {
                    baseReduction += 0.05; // Small bonus for iron
                }

                damage = 12.0 * (1.0 - baseReduction);
                damage = Math.max(4.0, damage); // Minimum 2 hearts with any armor
            }
        }

        return damage;
    }
}