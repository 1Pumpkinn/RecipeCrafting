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

        // Always put ability on cooldown + show activation message
        player.sendMessage(Component.text("🌿 Vine Trap activated!")
                .color(NamedTextColor.GREEN));
        setCooldown(player);

        // Try to find target
        LivingEntity target = getTargetLivingEntity(player);

        // If no target, just end here (ability still used)
        if (target == null) {
            return;
        }

        // Prevent self-targeting
        if (target.equals(player)) {
            return;
        }

        // Prevent targeting trusted players
        if (target instanceof Player && trustManager.isTrusted(player, (Player) target)) {
            return;
        }

        // Prevent re-trapping
        if (isEntityTrapped(target.getUniqueId())) {
            return;
        }

        // Trap the target
        trapEntity(target, 5);

        if (target instanceof Player) {
            ((Player) target).sendMessage(Component.text("🌿 You have been trapped by vines!")
                    .color(NamedTextColor.DARK_GREEN));
        }
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

        // Apply slowness and jump boost negative effects to all entities
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, 255, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationSeconds * 20, -10, false, false));

        // Calculate trap end time
        long trapEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Create repeating task to teleport entity back to trap location every 2 ticks
        BukkitTask teleportTask = null;
        if (pluginInstance != null) {
            teleportTask = pluginInstance.getServer().getScheduler().runTaskTimer(pluginInstance, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    Location currentLoc = entity.getLocation();
                    Location trap = trapLocation.clone();

                    trap.setYaw(currentLoc.getYaw());
                    trap.setPitch(currentLoc.getPitch());

                    if (currentLoc.distance(trapLocation) > 0.5) {
                        entity.teleport(trap);
                        entity.setVelocity(entity.getVelocity().multiply(0));
                    }
                }
            }, 0L, 2L);
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
                    entity.removePotionEffect(PotionEffectType.SLOWNESS);
                    entity.removePotionEffect(PotionEffectType.JUMP_BOOST);

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
