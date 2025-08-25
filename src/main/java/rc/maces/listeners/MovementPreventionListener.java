package rc.maces.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import rc.maces.abilities.earth.VinePullAbility;

import java.util.UUID;

/**
 * Listener to prevent movement for entities trapped by abilities like Vine Pull
 */
public class MovementPreventionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Check if this player is trapped by Vine Pull
        if (VinePullAbility.isEntityTrapped(playerId)) {
            Location trapLocation = VinePullAbility.getTrapLocation(playerId);

            if (trapLocation != null) {
                // Cancel the movement by setting the "to" location to the trap location
                Location from = event.getFrom();
                Location to = event.getTo();

                // Only prevent horizontal movement, allow looking around
                if (to != null && (to.getX() != from.getX() || to.getZ() != from.getZ() || Math.abs(to.getY() - from.getY()) > 0.1)) {
                    // Set the destination to the trap location but preserve head movement
                    Location newTo = trapLocation.clone();
                    newTo.setYaw(to.getYaw());
                    newTo.setPitch(to.getPitch());

                    event.setTo(newTo);

                    // Cancel any velocity to prevent jumping or other movement
                    event.getPlayer().setVelocity(new Vector(0, 0, 0));
                }
            }
        }
    }
}