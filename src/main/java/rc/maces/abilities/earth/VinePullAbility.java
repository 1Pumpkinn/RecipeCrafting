package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Vine Pull Ability - Pulls all living entities towards you and completely immobilizes them
public class VinePullAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public VinePullAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("vine_pull", 25, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        Map<LivingEntity, Location> entangledEntities = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    // Clear entangled entities map
                    entangledEntities.clear();
                    cancel();
                    return;
                }

                // Pull all living entities towards center every 5 ticks
                if (ticks % 5 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3, 3, 3);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Calculate direction towards center
                            Vector direction = center.toVector()
                                    .subtract(target.getLocation().toVector())
                                    .normalize()
                                    .multiply(1.2);

                            target.setVelocity(direction);

                            // Store the target's location for complete immobilization
                            Location immobilizeLocation = target.getLocation().clone();
                            entangledEntities.put(target, immobilizeLocation);

                            // Complete immobilization - maximum strength effects
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255)); // Can't move
                            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 255)); // Can't break blocks
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 255)); // Maximum weakness
                            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, -10)); // Prevents jumping (negative effect)

                            // Additional stunning effects
                            if (target instanceof Player) {
                                Player targetPlayer = (Player) target;
                                // Prevent any movement by setting walk speed to 0
                                targetPlayer.setWalkSpeed(0.0f);
                                targetPlayer.setFlySpeed(0.0f);
                            }

                            // Add vine particles around target instead of placing blocks
                            Location targetLoc = target.getLocation();
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    Location particleLoc = targetLoc.clone().add(x, 0, z);
                                    targetLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, particleLoc, 5, Material.VINE.createBlockData());
                                }
                            }

                            // Send message only to players
                            if (target instanceof Player) {
                                ((Player) target).sendMessage(Component.text("🌿 Completely entangled by vines! You are stunned!")
                                        .color(NamedTextColor.DARK_GREEN));
                            }
                        }
                    }
                }

                // Force entangled entities to stay in their immobilized position every tick
                for (Map.Entry<LivingEntity, Location> entry : entangledEntities.entrySet()) {
                    LivingEntity entity = entry.getKey();
                    Location immobilizeLocation = entry.getValue();

                    // Teleport them back if they've moved more than a small threshold
                    if (entity.getLocation().distance(immobilizeLocation) > 0.1) {
                        entity.teleport(immobilizeLocation);
                        entity.setVelocity(new Vector(0, 0, 0)); // Cancel any velocity
                    }
                }

                ticks++;
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                // Restore movement speed for all affected players
                for (LivingEntity entity : entangledEntities.keySet()) {
                    if (entity instanceof Player) {
                        Player targetPlayer = (Player) entity;
                        targetPlayer.setWalkSpeed(0.2f); // Default walk speed
                        targetPlayer.setFlySpeed(0.1f); // Default fly speed
                    }
                }
                super.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("🌿 VINE PULL! Pulling enemies into you and stunning them for 5 seconds!")
                .color(NamedTextColor.GREEN));
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_SPIDER_STEP, 1.5f, 0.8f);

        setCooldown(player);
    }
}
