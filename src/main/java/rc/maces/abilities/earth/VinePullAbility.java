package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.HashSet;
import java.util.Set;

// Vine Pull Ability - Pulls all living entities towards you and immobilizes them
public class VinePullAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public VinePullAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("vine_pull", 20, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        Set<Location> vineLocations = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    // Remove all vines
                    for (Location vineLoc : vineLocations) {
                        if (vineLoc.getBlock().getType() == Material.VINE) {
                            vineLoc.getBlock().setType(Material.AIR);
                        }
                    }
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

                            // Immobilize target for 2 seconds
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255)); // Can't move
                            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 255)); // Can't break blocks
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1)); // Reduced damage

                            // Place vines around target temporarily
                            Location targetLoc = target.getLocation();
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    Location vineLoc = targetLoc.clone().add(x, 0, z);
                                    if (vineLoc.getBlock().getType() == Material.AIR) {
                                        vineLoc.getBlock().setType(Material.VINE);
                                        vineLocations.add(vineLoc);
                                    }
                                }
                            }

                            // Send message only to players
                            if (target instanceof Player) {
                                ((Player) target).sendMessage(Component.text("🌿 Entangled by vines! You can't move!")
                                        .color(NamedTextColor.GREEN));
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("🌿 VINE PULL! Dragging enemies towards you and immobilizing them!")
                .color(NamedTextColor.GREEN));
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 2.0f, 0.6f);

        setCooldown(player);
    }
}