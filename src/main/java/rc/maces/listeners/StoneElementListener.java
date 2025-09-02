package rc.maces.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import rc.maces.managers.ElementManager;

import java.util.Random;

public class StoneElementListener implements Listener {

    private final ElementManager elementManager;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public StoneElementListener(ElementManager elementManager, JavaPlugin plugin) {
        this.elementManager = elementManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        String victimElement = elementManager.getPlayerElement(victim);

        // Stone element: 1% chance for Resistance 5 when hit
        if ("STONE".equals(victimElement)) {
            if (random.nextDouble() < 0.01) { // 1% chance
                victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4)); // 5 seconds, level 5
                victim.sendMessage(net.kyori.adventure.text.Component.text("🗿 Stone Shield activated!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.5f);

                plugin.getLogger().info(victim.getName() + " triggered Stone Shield (1% chance)");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerElement = elementManager.getPlayerElement(player);

        // Handle Stone Core drop on death
        if ("STONE".equals(playerElement)) {
            elementManager.handleStoneCoreDrop(player);
        }
    }
}