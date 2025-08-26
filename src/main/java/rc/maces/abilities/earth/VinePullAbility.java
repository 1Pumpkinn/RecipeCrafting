package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// FIXED Vine Trap Ability - Now works on ALL living entities with proper range (10 blocks)
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
        // Check cooldown first
        if (!canUse(player)) {
            return; // Don't spam chat with cooldown messages
        }

        // Get target living entity (including mobs)
        LivingEntity target = getTargetLivingEntity(player);

        if (target == null) {
            return; // Don't spam chat if no target
        }

        // Check if target is the same as caster
        if (target.equals(player)) {
            return; // Don't spam chat
        }

        // Check trust/permission - prevent using ability on trusted allies (only for players)
        if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
            return; // Don't spam chat
        }

        // Check if target is already trapped
        if (isEntityTrapped(target.getUniqueId())) {
            return; // Don't spam chat
        }

        // Trap the target for 5 seconds
        trapEntity(target, 5);

        // Send messages only once per use
        player.sendMessage(Component.text("🌿 Vine Trap activated!")
                .color(NamedTextColor.GREEN));

        if (target instanceof Player) {
            ((Player) target).sendMessage(Component.text("🌿 You have been trapped by vines!")
                    .color(NamedTextColor.DARK_GREEN));
        }

        setCooldown(player);
    }

    /**
     * Get the target living entity for the ability using ray tracing (10 block range)
     */
    private LivingEntity getTargetLivingEntity(Player caster) {
        // Use ray tracing to find the target living entity within 10 blocks
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                10.0, // FIXED: 10 block range
                entity -> entity instanceof LivingEntity && !entity.equals(caster)
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
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
     * Trap a living entity at their current location
     * @param entity The living entity to trap
     * @param durationSeconds How long to trap them for
     */
    public static void trapEntity(LivingEntity entity, int durationSeconds) {
        UUID entityId = entity.getUniqueId();
        Location trapLocation = entity.getLocation();

        // Store original speeds (only for players)
        float originalWalkSpeed = 0.2f; // Default walk speed
        float originalFlySpeed = 0.1f; // Default fly speed

        if (entity instanceof Player) {
            Player player = (Player) entity;
            originalWalkSpeed = player.getWalkSpeed();
            originalFlySpeed = player.getFlySpeed();
        }

        // Calculate trap end time
        long trapEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Store trapped data
        trappedEntities.put(entityId, new VineTrappedData(
                trapLocation, originalWalkSpeed, originalFlySpeed, trapEndTime
        ));

        // Set speeds to 0 to prevent movement (only for players)
        if (entity instanceof Player) {
            Player player = (Player) entity;
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
        }

        // Schedule automatic release
        if (pluginInstance != null) {
            pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                releaseEntity(entityId);
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
            // Restore original speeds only for players
            if (pluginInstance != null) {
                Player player = pluginInstance.getServer().getPlayer(entityId);
                if (player != null && player.isOnline()) {
                    player.setWalkSpeed(data.originalWalkSpeed);
                    player.setFlySpeed(data.originalFlySpeed);
                    player.sendMessage(Component.text("🌿 You have been freed from the vines!")
                            .color(NamedTextColor.GREEN));
                }
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