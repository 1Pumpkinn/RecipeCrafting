package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.MaceManager;

public class ActionBarTask extends BukkitRunnable {

    private final MaceManager maceManager;

    public ActionBarTask(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String actionBarText = maceManager.getActionBarStatus(player);
            if (!actionBarText.isEmpty()) {
                player.sendActionBar(Component.text(actionBarText));
            }
        }
    }
}