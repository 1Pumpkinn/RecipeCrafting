package rc.maces.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public void setCooldown(UUID playerId, String ability, long duration) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(ability, System.currentTimeMillis() + duration);
    }

    public boolean isOnCooldown(UUID playerId, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;

        Long cooldownEnd = playerCooldowns.get(ability);
        if (cooldownEnd == null) return false;

        if (System.currentTimeMillis() >= cooldownEnd) {
            playerCooldowns.remove(ability);
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(UUID playerId, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;

        Long cooldownEnd = playerCooldowns.get(ability);
        if (cooldownEnd == null) return 0;

        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void removeCooldown(UUID playerId, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(ability);
        }
    }

    public void clearAllCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
}