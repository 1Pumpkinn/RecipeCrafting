package rc.maces.abilities.water;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.Collection;

// WaterGeyser Ability - Launches ALL nearby living entities upwards (except allies)
public class WaterGeyserAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    public WaterGeyserAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("water_geyser", 30, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();

        // Create geyser effect
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds
                    cancel();
                    return;
                }

                // Visual effects
                for (int y = 0; y < 8; y++) {
                    Location effectLoc = center.clone().add(0, y, 0);
                    effectLoc.getWorld().spawnParticle(Particle.SPLASH, effectLoc, 15);
                    effectLoc.getWorld().spawnParticle(Particle.BUBBLE, effectLoc, 8);
                }

                // Launch ALL living entities (players and mobs) every 5 ticks except allies
                if (ticks % 5 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3, 3, 3);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Check trust system - don't launch trusted players
                            if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                                continue;
                            }

                            target.setVelocity(new Vector(0, 3.0, 0));

                            // Send message only to players
                            if (target instanceof Player) {
                                ((Player) target).sendMessage(Component.text("🌊 Launched by Water Geyser!")
                                        .color(NamedTextColor.BLUE));
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("🌊 WATER GEYSER! Launching enemies skyward!")
                .color(NamedTextColor.BLUE));
        center.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 1.2f);

        setCooldown(player);
    }
}