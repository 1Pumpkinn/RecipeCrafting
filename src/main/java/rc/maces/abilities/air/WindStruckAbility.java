package rc.maces.abilities.air;

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
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.Collection;

// UPDATED WindStruck Ability - Creates cobwebs and applies slow falling in 8x8 area to ALL living entities (except allies)
public class WindStruckAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    public WindStruckAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("wind_struck", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        int affectedTargets = 0;

        // UPDATED: Apply effects to ALL living entities (players and mobs) in 8x8 area (except allies)
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0); // 8x8 area (4 block radius)
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check trust system - don't affect trusted players
                if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                    continue;
                }

                // Apply slow falling for 5 seconds to ALL living entities
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));

                // Send message only to players
                if (target instanceof Player) {
                    ((Player) target).sendMessage(Component.text("💨 You've been Wind Struck!")
                            .color(NamedTextColor.GRAY));
                }

                affectedTargets++;
            }
        }

        // Place cobwebs on affected enemies to trap them
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Check trust system - don't trap trusted players
                if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
                    continue;
                }

                Location targetLoc = target.getLocation();

                // Place cobwebs around the enemy to trap them
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location cobwebLoc = targetLoc.clone().add(x, 0, z);
                        if (cobwebLoc.getBlock().getType() == Material.AIR) {
                            cobwebLoc.getBlock().setType(Material.COBWEB);

                            // Remove cobweb after 3 seconds
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (cobwebLoc.getBlock().getType() == Material.COBWEB) {
                                        cobwebLoc.getBlock().setType(Material.AIR);
                                    }
                                }
                            }.runTaskLater(plugin, 60L);
                        }
                    }
                }
            }
        }

        player.sendMessage(Component.text("💨 WIND STRUCK! Trapped " + affectedTargets + " enemies in webs!")
                .color(NamedTextColor.WHITE));
        center.getWorld().playSound(center, Sound.BLOCK_COBWEB_PLACE, 1.5f, 1.5f);

        setCooldown(player);
    }
}