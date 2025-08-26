package rc.maces.listeners;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import rc.maces.abilities.earth.VineTrapAbility;

import java.util.UUID;

/**
 * Listener to prevent movement for entities trapped by abilities like Vine Trap
 */
public class MovementPreventionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Check if this player is trapped by Vine Trap
        if (VineTrapAbility.isEntityTrapped(playerId)) {
            Location trapLocation = VineTrapAbility.getTrapLocation(playerId);

            if (trapLocation != null) {
                Location from = event.getFrom();
                Location to = event.getTo();

                // Only prevent horizontal movement and significant vertical movement, allow looking around
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

    // Handle entity movement for non-player entities (if EntityMoveEvent exists in your server version)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();

        // Check if this entity is trapped by Vine Trap
        if (VineTrapAbility.isEntityTrapped(entityId)) {
            Location trapLocation = VineTrapAbility.getTrapLocation(entityId);

            if (trapLocation != null) {
                Location from = event.getFrom();
                Location to = event.getTo();

                // Prevent any movement from the trap location
                if (from.distance(trapLocation) > 0.5 || to.distance(trapLocation) > 0.5) {
                    // Cancel the movement
                    event.setCancelled(true);

                    // Teleport back to trap location
                    entity.teleport(trapLocation);
                    entity.setVelocity(new Vector(0, 0, 0));
                }
            }
        }
    }

    /**
     * Clean up trapped entities when players quit to prevent memory leaks
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Release the player if they're trapped when they quit
        if (VineTrapAbility.isEntityTrapped(playerId)) {
            VineTrapAbility.releaseEntity(playerId);
        }
    }
}