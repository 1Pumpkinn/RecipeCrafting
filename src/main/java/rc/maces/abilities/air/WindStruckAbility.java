package rc.maces.abilities.air;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.Collection;

// WindStruck Ability - Creates cobwebs and applies slow falling in 5x5 area
public class WindStruckAbility extends BaseAbility {

    private final JavaPlugin plugin;

    public WindStruckAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("wind_struck", 20, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        Location center = player.getLocation();

        // Apply effects to players in 5x5 area
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;

                // Apply slow falling for 5 seconds
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));

                // Place cobwebs at their location temporarily
                Location targetLoc = target.getLocation();
                if (targetLoc.getBlock().getType() == Material.AIR) {
                    targetLoc.getBlock().setType(Material.COBWEB);

                    // Remove cobweb after 3 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (targetLoc.getBlock().getType() == Material.COBWEB) {
                                targetLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }.runTaskLater(plugin, 60L);
                }

                target.sendMessage(Component.text("💨 You've been Wind Struck!")
                        .color(NamedTextColor.GRAY));
            }
        }

        player.sendMessage(Component.text("💨 WIND STRUCK! Trapped enemies in webs!")
                .color(NamedTextColor.WHITE));
        center.getWorld().playSound(center, Sound.BLOCK_COBWEB_PLACE, 1.5f, 0.8f);

        setCooldown(player);
    }
}