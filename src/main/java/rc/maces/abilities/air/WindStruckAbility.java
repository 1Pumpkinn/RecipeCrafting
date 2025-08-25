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

import java.util.Collection;

// WindStruck Ability - Creates cobwebs and applies slow falling in 5x5 area to ALL living entities
public class WindStruckAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public WindStruckAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("wind_struck", 25, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();
        int affectedTargets = 0;

        // Apply effects to ALL living entities (players and mobs) in 5x5 area
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                // Apply slow falling for 5 seconds to ALL living entities
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));

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