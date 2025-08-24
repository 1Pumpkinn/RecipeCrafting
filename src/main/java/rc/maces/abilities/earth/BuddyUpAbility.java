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

// BuddyUp Ability - Summons a protective iron golem that attacks ALL living entities except summoner
public class BuddyUpAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private static final Map<UUID, IronGolem> playerGolems = new HashMap<>();
    private static final Set<UUID> golemUUIDs = new HashSet<>();

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

        // Store both the golem reference and its UUID to track it
        playerGolems.put(player.getUniqueId(), golem);
        golemUUIDs.add(golem.getUniqueId());

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
            golemUUIDs.remove(existingGolem.getUniqueId());
            existingGolem.remove();
        }
    }

    // Handle when the player gets damaged by ANY living entity, but prevent golem from attacking summoner
    public static void handlePlayerDamage(EntityDamageByEntityEvent event, Player victim) {
        IronGolem golem = playerGolems.get(victim.getUniqueId());
        if (golem != null && !golem.isDead() && event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();

            // Don't make golem attack its own summoner
            if (attacker instanceof Player && attacker.equals(victim)) {
                return;
            }

            // Don't make golem attack other golems (prevent golem wars)
            if (attacker instanceof IronGolem && golemUUIDs.contains(attacker.getUniqueId())) {
                return;
            }

            // Make golem target the attacker (works on ALL living entities except summoner)
            golem.setTarget(attacker);
        }
    }

    // Handle when a golem gets damaged - prevent it from attacking its own summoner
    public static void handleGolemDamage(EntityDamageByEntityEvent event, IronGolem golem) {
        if (!golemUUIDs.contains(golem.getUniqueId())) {
            return; // Not one of our custom golems
        }

        // Find the summoner of this golem
        Player summoner = null;
        for (Map.Entry<UUID, IronGolem> entry : playerGolems.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(golem)) {
                summoner = org.bukkit.Bukkit.getPlayer(entry.getKey());
                break;
            }
        }

        // If the summoner is attacking their own golem, prevent the golem from retaliating
        if (summoner != null && event.getDamager().equals(summoner)) {
            // Clear the golem's target if it's targeting its summoner
            if (golem.getTarget() != null && golem.getTarget().equals(summoner)) {
                golem.setTarget(null);
            }
        }
    }

    // Clean up method to be called when golems die naturally
    public static void cleanupGolem(IronGolem golem) {
        UUID golemUUID = golem.getUniqueId();
        golemUUIDs.remove(golemUUID);

        // Find and remove from playerGolems map
        playerGolems.entrySet().removeIf(entry -> {
            IronGolem playerGolem = entry.getValue();
            return playerGolem != null && playerGolem.getUniqueId().equals(golemUUID);
        });
    }
}