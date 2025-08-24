package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.*;

// BuddyUp Ability - Summons a protective iron golem
public class BuddyUpAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private static final Map<UUID, IronGolem> playerGolems = new HashMap<>();

    public BuddyUpAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("buddy_up", 15, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        // Remove existing golem if any
        removeExistingGolem(player);

        Location spawnLoc = player.getLocation().add(2, 0, 0);
        IronGolem golem = (IronGolem) player.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);

        // Set health to 10 hearts (20 HP)
        golem.setMaxHealth(20.0);
        golem.setHealth(20.0);

        // Make it protect the player
        golem.setCustomName("§a" + player.getName() + "'s Buddy");
        golem.setCustomNameVisible(true);

        playerGolems.put(player.getUniqueId(), golem);

        player.sendMessage(Component.text("🤖 BUDDY UP! Your iron golem protector has arrived!")
                .color(NamedTextColor.GREEN));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.8f);

        // Remove golem after 60 seconds if still alive
        new BukkitRunnable() {
            @Override
            public void run() {
                removeExistingGolem(player);
            }
        }.runTaskLater(plugin, 1200L);

        setCooldown(player);
    }

    private void removeExistingGolem(Player player) {
        IronGolem existingGolem = playerGolems.remove(player.getUniqueId());
        if (existingGolem != null && !existingGolem.isDead()) {
            existingGolem.remove();
        }
    }

    // Handle when the player gets damaged by any living entity
    public static void handlePlayerDamage(EntityDamageByEntityEvent event, Player victim) {
        IronGolem golem = playerGolems.get(victim.getUniqueId());
        if (golem != null && !golem.isDead() && event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();
            // Make golem target the attacker (works on all living entities)
            golem.setTarget(attacker);
        }
    }
}