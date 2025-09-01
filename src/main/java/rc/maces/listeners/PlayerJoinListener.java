package rc.maces.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rc.maces.managers.ElementManager;

public class PlayerJoinListener implements Listener {

    private final ElementManager elementManager;

    public PlayerJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Assign element to player (will use existing one if they have it, or assign new random one)
        elementManager.assignRandomElement(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Do nothing - we want to keep element data persistent
        // Data is saved to file automatically when assigned/changed
    }
}