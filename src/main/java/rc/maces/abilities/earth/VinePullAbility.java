package rc.maces.abilities.earth;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// FIXED Vine Trap Ability (formerly Vine Pull) - Now completely immobilizes entities without screen jitter
public class VinePullAbility extends BaseAbility {

    private final JavaPlugin plugin;
    private final TrustManager trustManager;

    // Static tracking to prevent movement across all instances
    private static final Map<UUID, VineTrappedData> trappedEntities = new HashMap<>();
    private static JavaPlugin pluginInstance;

    // Helper class to store trapped entity data
    private static class VineTrappedData {
        final Location trapLocation;
        final float originalWalkSpeed;
        final float originalFlySpeed;
        final long trapEndTime;

        VineTrappedData(Location location, float walkSpeed, float flySpeed, long endTime) {
            this.trapLocation = location.clone();
            this.originalWalkSpeed = walkSpeed;
            this.originalFlySpeed = flySpeed;
            this.trapEndTime = endTime;
        }
    }

    public VinePullAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("vine_pull", 25, cooldownManager);
        this.plugin = plugin;
        this.trustManager = trustManager;

        // Set the static plugin instance
        VinePullAbility.pluginInstance = plugin;
    }

    @Override
    public void execute(Player player) {
        // Check if player can use the ability (cooldown check)
        if (cooldownManager.isOnCooldown(player, "vine_pull")) {
            int remainingSeconds = cooldownManager.getRemainingCooldownSeconds(player, "vine_pull");
            player.sendMessage("Vine Pull is on cooldown for " + remainingSeconds + " more seconds!");
            return;
        }

        // Get target player
        Player target = getTargetPlayer(player);

        if (target == null) {
            player.sendMessage("No valid target found!");
            return;
        }

        // Check if target is the same as caster
        if (target.equals(player)) {
            player.sendMessage("You cannot use this ability on yourself!");
            return;
        }

        // Check trust/permission - prevent using ability on trusted allies
        if (trustManager.isTrusted(player, target)) {
            player.sendMessage("You cannot use this ability on " + target.getName() + " - you are allies!");
            return;
        }

        // Check if target is already trapped
        if (isEntityTrapped(target.getUniqueId())) {
            player.sendMessage(target.getName() + " is already trapped!");
            return;
        }

        // Trap the target for 5 seconds (adjust as needed)
        trapEntity(target, 5);

        // Send messages
        player.sendMessage("You have trapped " + target.getName() + " with vines!");
        target.sendMessage("You have been trapped by vines by " + player.getName() + "!");

        // Apply cooldown
        cooldownManager.setCooldown(player, "vine_pull", 25 * 1000L); // 25 seconds
    }

    /**
     * Get the target player for the ability using ray tracing
     */
    private Player getTargetPlayer(Player caster) {
        // Use ray tracing to find the target player within 10 blocks
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                10.0, // max distance
                entity -> entity instanceof Player && !entity.equals(caster)
        );

        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player) result.getHitEntity();
        }

        return null;
    }

    // ==================== STATIC METHODS ====================

    /**
     * Check if an entity is currently trapped by Vine Pull
     * @param entityId The UUID of the entity to check
     * @return true if the entity is trapped, false otherwise
     */
    public static boolean isEntityTrapped(UUID entityId) {
        if (!trappedEntities.containsKey(entityId)) {
            return false;
        }

        VineTrappedData data = trappedEntities.get(entityId);

        // Check if the trap has expired
        if (System.currentTimeMillis() > data.trapEndTime) {
            // Clean up expired trap
            releaseEntity(entityId);
            return false;
        }

        return true;
    }

    /**
     * Get the trap location for a trapped entity
     * @param entityId The UUID of the entity
     * @return The location where the entity is trapped, or null if not trapped
     */
    public static Location getTrapLocation(UUID entityId) {
        VineTrappedData data = trappedEntities.get(entityId);
        return data != null ? data.trapLocation.clone() : null;
    }

    /**
     * Trap an entity at their current location
     * @param player The player to trap
     * @param durationSeconds How long to trap them for
     */
    public static void trapEntity(Player player, int durationSeconds) {
        UUID playerId = player.getUniqueId();
        Location trapLocation = player.getLocation();

        // Store original speeds
        float originalWalkSpeed = player.getWalkSpeed();
        float originalFlySpeed = player.getFlySpeed();

        // Calculate trap end time
        long trapEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Store trapped data
        trappedEntities.put(playerId, new VineTrappedData(
                trapLocation, originalWalkSpeed, originalFlySpeed, trapEndTime
        ));

        // Set speeds to 0 to prevent movement
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);

        // Schedule automatic release
        if (pluginInstance != null) {
            pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                releaseEntity(playerId);
            }, durationSeconds * 20L); // Convert seconds to ticks
        }
    }

    /**
     * Release a trapped entity
     * @param entityId The UUID of the entity to release
     */
    public static void releaseEntity(UUID entityId) {
        VineTrappedData data = trappedEntities.remove(entityId);

        if (data != null) {
            // Restore original speeds if player is still online
            Player player = pluginInstance != null ? pluginInstance.getServer().getPlayer(entityId) : null;
            if (player != null && player.isOnline()) {
                player.setWalkSpeed(data.originalWalkSpeed);
                player.setFlySpeed(data.originalFlySpeed);
                player.sendMessage("You have been freed from the vines!");
            }
        }
    }

    /**
     * Get all currently trapped entities (for cleanup/debugging)
     * @return Map of trapped entities
     */
    public static Map<UUID, VineTrappedData> getTrappedEntities() {
        return new HashMap<>(trappedEntities);
    }

    /**
     * Clean up expired traps (should be called periodically)
     */
    public static void cleanupExpiredTraps() {
        long currentTime = System.currentTimeMillis();
        trappedEntities.entrySet().removeIf(entry -> {
            boolean expired = currentTime > entry.getValue().trapEndTime;
            if (expired) {
                // Restore player speeds if they're still online
                Player player = pluginInstance != null ? pluginInstance.getServer().getPlayer(entry.getKey()) : null;
                if (player != null && player.isOnline()) {
                    player.setWalkSpeed(entry.getValue().originalWalkSpeed);
                    player.setFlySpeed(entry.getValue().originalFlySpeed);
                    player.sendMessage("You have been freed from the vines!");
                }
            }
            return expired;
        });
    }

    /**
     * Force release all trapped entities (useful for plugin disable)
     */
    public static void releaseAllEntities() {
        for (UUID entityId : trappedEntities.keySet()) {
            releaseEntity(entityId);
        }
        trappedEntities.clear();
    }

    /**
     * Check if any entities are currently trapped
     * @return true if there are trapped entities, false otherwise
     */
    public static boolean hasTrappedEntities() {
        return !trappedEntities.isEmpty();
    }

    /**
     * Get the remaining trap time for an entity in seconds
     * @param entityId The UUID of the entity
     * @return remaining time in seconds, or -1 if not trapped
     */
    public static int getRemainingTrapTime(UUID entityId) {
        VineTrappedData data = trappedEntities.get(entityId);
        if (data == null) {
            return -1;
        }

        long remainingMs = data.trapEndTime - System.currentTimeMillis();
        return Math.max(0, (int) (remainingMs / 1000));
    }
}