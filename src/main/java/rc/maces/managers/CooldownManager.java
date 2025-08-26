package rc.maces.managers;

import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fixed CooldownManager with proper cooldown handling
 */
public class CooldownManager {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Set a cooldown for a specific player and ability
     */
    public void setCooldown(UUID playerId, String ability, long cooldownMs) {
        if (playerId == null || ability == null || ability.isEmpty()) {
            return;
        }

        String key = createKey(playerId, ability);
        cooldowns.put(key, System.currentTimeMillis() + cooldownMs);
    }

    /**
     * Set cooldown using Player object
     */
    public void setCooldown(Player player, String ability, long cooldownMs) {
        if (player == null) return;
        setCooldown(player.getUniqueId(), ability, cooldownMs);
    }

    /**
     * Check if a player has an active cooldown for an ability
     */
    public boolean isOnCooldown(UUID playerId, String ability) {
        if (playerId == null || ability == null || ability.isEmpty()) {
            return false;
        }

        String key = createKey(playerId, ability);
        Long expireTime = cooldowns.get(key);

        if (expireTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expireTime) {
            cooldowns.remove(key);
            return false;
        }

        return true;
    }

    /**
     * Check cooldown using Player object
     */
    public boolean isOnCooldown(Player player, String ability) {
        if (player == null) return false;
        return isOnCooldown(player.getUniqueId(), ability);
    }

    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(UUID playerId, String ability) {
        if (playerId == null || ability == null || ability.isEmpty()) {
            return 0;
        }

        String key = createKey(playerId, ability);
        Long expireTime = cooldowns.get(key);

        if (expireTime == null) {
            return 0;
        }

        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }

        return remaining;
    }

    /**
     * Get remaining cooldown using Player object
     */
    public long getRemainingCooldown(Player player, String ability) {
        if (player == null) return 0;
        return getRemainingCooldown(player.getUniqueId(), ability);
    }

    /**
     * Get remaining cooldown in seconds (rounded up)
     */
    public int getRemainingCooldownSeconds(UUID playerId, String ability) {
        long remainingMs = getRemainingCooldown(playerId, ability);
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    /**
     * Get remaining cooldown in seconds using Player object
     */
    public int getRemainingCooldownSeconds(Player player, String ability) {
        if (player == null) return 0;
        return getRemainingCooldownSeconds(player.getUniqueId(), ability);
    }

    /**
     * Clear a specific cooldown
     */
    public void clearCooldown(UUID playerId, String ability) {
        if (playerId == null || ability == null || ability.isEmpty()) {
            return;
        }

        String key = createKey(playerId, ability);
        cooldowns.remove(key);
    }

    /**
     * Clear cooldown using Player object
     */
    public void clearCooldown(Player player, String ability) {
        if (player == null) return;
        clearCooldown(player.getUniqueId(), ability);
    }

    /**
     * Clear all cooldowns for a player
     */
    public void clearAllCooldowns(UUID playerId) {
        if (playerId == null) return;

        String playerPrefix = playerId.toString() + ":";
        cooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
    }

    /**
     * Clear all cooldowns using Player object
     */
    public void clearAllCooldowns(Player player) {
        if (player == null) return;
        clearAllCooldowns(player.getUniqueId());
    }

    /**
     * Clean up expired cooldowns (call periodically for memory management)
     */
    public int cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;

        var iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentTime >= entry.getValue()) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    /**
     * Get total number of active cooldowns
     */
    public int getActiveCooldownCount() {
        cleanupExpiredCooldowns(); // Clean up expired ones first
        return cooldowns.size();
    }

    /**
     * Check if player can use ability (inverse of isOnCooldown)
     */
    public boolean canUseAbility(Player player, String ability) {
        return !isOnCooldown(player, ability);
    }

    /**
     * Create a consistent key for the cooldown map
     */
    private String createKey(UUID playerId, String ability) {
        return playerId.toString() + ":" + ability.toLowerCase();
    }

    /**
     * Get all active cooldowns for debugging
     */
    public Map<String, Long> getActiveCooldowns() {
        cleanupExpiredCooldowns();
        return new ConcurrentHashMap<>(cooldowns);
    }
}