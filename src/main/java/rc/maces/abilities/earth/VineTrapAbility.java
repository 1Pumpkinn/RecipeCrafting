package rc.maces.abilities.earth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import rc.maces.abilities.BaseAbility;
import rc.maces.managers.CooldownManager;
import rc.maces.managers.TrustManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// FIXED Vine Trap Ability - Now works on ALL living entities with proper immobilization
public class VineTrapAbility extends BaseAbility {

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
        final BukkitTask teleportTask;
        final BukkitTask releaseTask;

        VineTrappedData(Location location, float walkSpeed, float flySpeed, long endTime, BukkitTask teleportTask, BukkitTask releaseTask) {
            this.trapLocation = location.clone();
            this.originalWalkSpeed = walkSpeed;
            this.originalFlySpeed = flySpeed;
            this.trapEndTime = endTime;
            this.teleportTask = teleportTask;
            this.releaseTask = releaseTask;
        }
    }

    public VineTrapAbility(CooldownManager cooldownManager, JavaPlugin plugin, TrustManager trustManager) {
        super("vine_trap", 25, cooldownManager); // Changed from vine_pull
        this.plugin = plugin;
        this.trustManager = trustManager;

        // Set the static plugin instance
        VineTrapAbility.pluginInstance = plugin;
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
                10.0, // 10 block range
                entity -> entity instanceof LivingEntity && !entity.equals(caster)
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        return null;
    }

    // ==================== STATIC METHODS ====================

    /**
     * Check if an entity is currently trapped by Vine Trap
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
            // Set speeds to 0 to prevent movement
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
        }

        // Apply slowness and jump boost negative effects to all entities
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, 255, false, false)); // Max slowness
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationSeconds * 20, -10, false, false)); // Negative jump boost

        // Calculate trap end time
        long trapEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Create repeating task to teleport entity back to trap location every 2 ticks
        BukkitTask teleportTask = null;
        if (pluginInstance != null) {
            teleportTask = pluginInstance.getServer().getScheduler().runTaskTimer(pluginInstance, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    Location currentLoc = entity.getLocation();
                    Location trap = trapLocation.clone();

                    // Allow head movement but keep body at trap location
                    trap.setYaw(currentLoc.getYaw());
                    trap.setPitch(currentLoc.getPitch());

                    // Only teleport if entity has moved significantly from trap location
                    if (currentLoc.distance(trapLocation) > 0.5) {
                        entity.teleport(trap);
                        // Remove any velocity
                        entity.setVelocity(entity.getVelocity().multiply(0));
                    }
                }
            }, 0L, 2L); // Run every 2 ticks (0.1 seconds)
        }

        // Schedule automatic release
        BukkitTask releaseTask = null;
        if (pluginInstance != null) {
            releaseTask = pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                releaseEntity(entityId);
            }, durationSeconds * 20L); // Convert seconds to ticks
        }

        // Store trapped data
        trappedEntities.put(entityId, new VineTrappedData(
                trapLocation, originalWalkSpeed, originalFlySpeed, trapEndTime, teleportTask, releaseTask
        ));
    }

    /**
     * Release a trapped entity
     * @param entityId The UUID of the entity to release
     */
    public static void releaseEntity(UUID entityId) {
        VineTrappedData data = trappedEntities.remove(entityId);

        if (data != null) {
            // Cancel the teleport task
            if (data.teleportTask != null && !data.teleportTask.isCancelled()) {
                data.teleportTask.cancel();
            }

            // Cancel the release task
            if (data.releaseTask != null && !data.releaseTask.isCancelled()) {
                data.releaseTask.cancel();
            }

            // Find the entity and restore effects
            if (pluginInstance != null) {
                LivingEntity entity = null;

                // Try to find the entity in all worlds
                for (org.bukkit.World world : pluginInstance.getServer().getWorlds()) {
                    for (LivingEntity livingEntity : world.getLivingEntities()) {
                        if (livingEntity.getUniqueId().equals(entityId)) {
                            entity = livingEntity;
                            break;
                        }
                    }
                    if (entity != null) break;
                }

                if (entity != null && entity.isValid() && !entity.isDead()) {
                    // Remove trap effects
                    entity.removePotionEffect(PotionEffectType.SLOWNESS);
                    entity.removePotionEffect(PotionEffectType.JUMP_BOOST);

                    // Restore original speeds only for players
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        player.setWalkSpeed(data.originalWalkSpeed);
                        player.setFlySpeed(data.originalFlySpeed);
                        player.sendMessage(Component.text("🌿 You have been freed from the vines!")
                                .color(NamedTextColor.GREEN));
                    }
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
                // Use releaseEntity to properly clean up
                releaseEntity(entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Force release all trapped entities (useful for plugin disable)
     */
    public static void releaseAllEntities() {
        for (UUID entityId : new HashMap<>(trappedEntities).keySet()) {
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