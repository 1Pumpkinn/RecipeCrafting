package rc.maces.abilities.fire;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// FirePassthrough Ability - Makes fire damage true damage for 5 seconds
public class FirePassthroughAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private static final Set<UUID> firePassthroughPlayers = new HashSet<>();

    public FirePassthroughAbility(CooldownManager cooldownManager, JavaPlugin plugin) {
        super("fire_passthrough", 10, cooldownManager);
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        if (!canUse(player)) return;

        firePassthroughPlayers.add(player.getUniqueId());

        player.sendMessage(Component.text("🔥 FIRE PASSTHROUGH! Your fire damage is now TRUE DAMAGE!")
                .color(NamedTextColor.RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);

        // Remove effect after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                firePassthroughPlayers.remove(player.getUniqueId());
                if (player.isOnline()) {
                    player.sendMessage(Component.text("🔥 Fire Passthrough ended.")
                            .color(NamedTextColor.GOLD));
                }
            }
        }.runTaskLater(plugin, 100L);

        setCooldown(player);
    }

    public static boolean hasFirePassthrough(Player player) {
        return firePassthroughPlayers.contains(player.getUniqueId());
    }
}