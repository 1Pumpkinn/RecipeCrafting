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

// IMPROVED Vine Trap Ability - Better targeting and entity support
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
        super("vine_trap", 25, cooldownManager);
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

        // Always put ability on cooldown + show activation message
        player.sendMessage(Component.text("🌿 Vine Trap activated! Looking for target...")
                .color(NamedTextColor.GREEN));
        setCooldown(player);

        // Try to find target using improved targeting
        LivingEntity target = getTargetLivingEntity(player);

        // If no target found, inform player
        if (target == null) {
            player.sendMessage(Component.text("🌿 No valid target found within range!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Prevent self-targeting
        if (target.equals(player)) {
            player.sendMessage(Component.text("🌿 You cannot trap yourself!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Prevent targeting trusted players
        if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
            player.sendMessage(Component.text("🌿 You cannot trap your trusted ally " + ((Player) target).getName() + "!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Prevent re-trapping
        if (isEntityTrapped(target.getUniqueId())) {
            player.sendMessage(Component.text("🌿 " + getEntityName(target) + " is already trapped!")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Trap the target
        trapEntity(target, 5);

        // Success messages
        String targetName = getEntityName(target);
        player.sendMessage(Component.text("🌿 Successfully trapped " + targetName + " for 5 seconds!")
                .color(NamedTextColor.GREEN));

        if (target instanceof Player) {
            ((Player) target).sendMessage(Component.text("🌿 You have been trapped by vines from " + player.getName() + "!")
                    .color(NamedTextColor.DARK_GREEN));
        }
    }

    /**
     * Get the target living entity using improved ray tracing with better range and filtering
     */
    private LivingEntity getTargetLivingEntity(Player caster) {
        // Use ray tracing to find the target living entity within 15 blocks (increased range)
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                15.0, // Increased from 10 to 15 blocks
                0.2,   // Entity bounding box expansion
                entity -> {
                    // Must be a living entity
                    if (!(entity instanceof LivingEntity)) return false;

                    // Don't target the caster
                    if (entity.equals(caster)) return false;

                    LivingEntity living = (LivingEntity) entity;

                    // Don't target dead entities
                    if (living.isDead() || !living.isValid()) return false;

                    // Don't target already trapped entities
                    if (isEntityTrapped(living.getUniqueId())) return false;

                    // For players, check trust system
                    if (living instanceof Player) {
                        return !trustManager.isTrusted(caster, (Player) living);
                    }

                    // Allow all other living entities (mobs, animals, etc.)
                    return true;
                }
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        // IMPROVED: If ray tracing fails, try nearby entity detection
        return caster.getWorld().getNearbyEntities(caster.getLocation(), 8, 8, 8).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(living -> {
                    if (living.equals(caster)) return false;
                    if (living.isDead() || !living.isValid()) return false;
                    if (isEntityTrapped(living.getUniqueId())) return false;

                    // Check if player is looking roughly towards the entity
                    Location playerLoc = caster.getEyeLocation();
                    Location entityLoc = living.getLocation();

                    // Simple angle check - entity should be roughly in front of player
                    double angle = Math.toDegrees(playerLoc.getDirection().angle(
                            entityLoc.toVector().subtract(playerLoc.toVector()).normalize()));

                    if (angle > 60) return false; // Not looking towards entity

                    // Check distance
                    if (playerLoc.distance(entityLoc) > 8) return false;

                    // Trust check for players
                    if (living instanceof Player) {
                        return !trustManager.isTrusted(caster, (Player) living);
                    }

                    return true;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a display name for any living entity
     */
    private String getEntityName(LivingEntity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        } else if (entity.getCustomName() != null) {
            return entity.getCustomName();
        } else {
            // Convert entity type to friendly name
            String typeName = entity.getType().name().toLowerCase().replace("_", " ");
            return typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        }
    }

    // ==================== STATIC METHODS (Enhanced) ====================

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

    public static Location getTrapLocation(UUID entityId) {
        VineTrappedData data = trappedEntities.get(entityId);
        return data != null ? data.trapLocation.clone() : null;
    }

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

        // Apply effects to immobilize all living entities
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, 255, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationSeconds * 20, -10, false, false));

        // Calculate trap end time
        long trapEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Create repeating task to keep entity at trap location
        BukkitTask teleportTask = null;
        if (pluginInstance != null) {
            teleportTask = pluginInstance.getServer().getScheduler().runTaskTimer(pluginInstance, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    Location currentLoc = entity.getLocation();
                    Location trap = trapLocation.clone();

                    // Preserve head/body rotation for players
                    if (entity instanceof Player) {
                        trap.setYaw(currentLoc.getYaw());
                        trap.setPitch(currentLoc.getPitch());
                    }

                    // Teleport if entity moved too far from trap
                    if (currentLoc.distance(trapLocation) > 0.3) {
                        entity.teleport(trap);
                        entity.setVelocity(entity.getVelocity().multiply(0));
                    }
                }
            }, 0L, 1L); // Every tick for maximum effectiveness
        }

        // Schedule automatic release
        BukkitTask releaseTask = null;
        if (pluginInstance != null) {
            releaseTask = pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                releaseEntity(entityId);
            }, durationSeconds * 20L);
        }

        trappedEntities.put(entityId, new VineTrappedData(
                trapLocation, originalWalkSpeed, originalFlySpeed, trapEndTime, teleportTask, releaseTask
        ));

        // Log for debugging
        if (pluginInstance != null) {
            String entityName = entity instanceof Player ? ((Player) entity).getName() :
                    entity.getCustomName() != null ? entity.getCustomName() : entity.getType().name();
            pluginInstance.getLogger().info("Trapped entity: " + entityName + " at " +
                    trapLocation.getBlockX() + "," + trapLocation.getBlockY() + "," + trapLocation.getBlockZ());
        }
    }

    public static void releaseEntity(UUID entityId) {
        VineTrappedData data = trappedEntities.remove(entityId);

        if (data != null) {
            if (data.teleportTask != null && !data.teleportTask.isCancelled()) {
                data.teleportTask.cancel();
            }
            if (data.releaseTask != null && !data.releaseTask.isCancelled()) {
                data.releaseTask.cancel();
            }

            if (pluginInstance != null) {
                LivingEntity entity = null;

                // Find the entity across all worlds
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
                    // Remove potion effects
                    entity.removePotionEffect(PotionEffectType.SLOWNESS);
                    entity.removePotionEffect(PotionEffectType.JUMP_BOOST);

                    // Restore player speeds
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        player.setWalkSpeed(data.originalWalkSpeed);
                        player.setFlySpeed(data.originalFlySpeed);
                        player.sendMessage(Component.text("🌿 You have been freed from the vines!")
                                .color(NamedTextColor.GREEN));
                    }

                    // Log release
                    String entityName = entity instanceof Player ? ((Player) entity).getName() :
                            entity.getCustomName() != null ? entity.getCustomName() : entity.getType().name();
                    pluginInstance.getLogger().info("Released entity: " + entityName);
                }
            }
        }
    }

    // Utility methods remain the same
    public static Map<UUID, VineTrappedData> getTrappedEntities() {
        return new HashMap<>(trappedEntities);
    }

    public static void cleanupExpiredTraps() {
        long currentTime = System.currentTimeMillis();
        trappedEntities.entrySet().removeIf(entry -> {
            boolean expired = currentTime > entry.getValue().trapEndTime;
            if (expired) {
                releaseEntity(entry.getKey());
            }
            return expired;
        });
    }

    public static void releaseAllEntities() {
        for (UUID entityId : new HashMap<>(trappedEntities).keySet()) {
            releaseEntity(entityId);
        }
        trappedEntities.clear();
    }

    public static boolean hasTrappedEntities() {
        return !trappedEntities.isEmpty();
    }

    public static int getRemainingTrapTime(UUID entityId) {
        VineTrappedData data = trappedEntities.get(entityId);
        if (data == null) {
            return -1;
        }

        long remainingMs = data.trapEndTime - System.currentTimeMillis();
        return Math.max(0, (int) (remainingMs / 1000));
    }
}