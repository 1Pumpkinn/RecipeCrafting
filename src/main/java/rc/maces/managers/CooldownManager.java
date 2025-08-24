package rc.maces.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    public void setCooldown(UUID playerId, String ability, long cooldownMs) {
        String key = playerId.toString() + ":" + ability;
        cooldowns.put(key, System.currentTimeMillis() + cooldownMs);
    }

    public boolean isOnCooldown(UUID playerId, String ability) {
        String key = playerId.toString() + ":" + ability;
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

    public long getRemainingCooldown(UUID playerId, String ability) {
        String key = playerId.toString() + ":" + ability;
        Long expireTime = cooldowns.get(key);

        if (expireTime == null) {
            return 0;
        }

        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void clearCooldown(UUID playerId, String ability) {
        String key = playerId.toString() + ":" + ability;
        cooldowns.remove(key);
    }

    public void clearAllCooldowns(UUID playerId) {
        String playerPrefix = playerId.toString() + ":";
        cooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
    }
}