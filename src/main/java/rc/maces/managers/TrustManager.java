package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrustManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Set<UUID>> trustRelations = new HashMap<>();
    private final Map<UUID, UUID> pendingTrustRequests = new HashMap<>();
    private final File dataFile;
    private final FileConfiguration dataConfig;

    public TrustManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "trust.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadTrustData();
    }

    /**
     * Sends a trust request to another player
     */
    public void sendTrustRequest(Player requester, Player target) {
        if (requester.equals(target)) {
            requester.sendMessage(Component.text("❌ You cannot trust yourself!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (isTrusted(requester, target)) {
            requester.sendMessage(Component.text("❌ You already trust " + target.getName() + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Check if there's already a pending request
        if (pendingTrustRequests.containsKey(target.getUniqueId()) &&
                pendingTrustRequests.get(target.getUniqueId()).equals(requester.getUniqueId())) {
            requester.sendMessage(Component.text("❌ You already have a pending trust request to " + target.getName() + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Store the pending request
        pendingTrustRequests.put(target.getUniqueId(), requester.getUniqueId());

        // Send messages
        requester.sendMessage(Component.text("🤝 Trust request sent to " + target.getName() + "!")
                .color(NamedTextColor.GREEN));

        target.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        target.sendMessage(Component.text("🤝 TRUST REQUEST 🤝")
                .color(NamedTextColor.GOLD));
        target.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        target.sendMessage(Component.text(requester.getName() + " wants to trust you!")
                .color(NamedTextColor.YELLOW));
        target.sendMessage(Component.text("• No PvP damage between you")
                .color(NamedTextColor.GRAY));
        target.sendMessage(Component.text("• No ability effects on each other")
                .color(NamedTextColor.GRAY));
        target.sendMessage(Component.text("• No passive effects on each other")
                .color(NamedTextColor.GRAY));
        target.sendMessage(Component.text("Type '/trustaccept' to accept or '/trustdeny' to deny")
                .color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));

        // Auto-remove request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingTrustRequests.containsKey(target.getUniqueId()) &&
                    pendingTrustRequests.get(target.getUniqueId()).equals(requester.getUniqueId())) {
                pendingTrustRequests.remove(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage(Component.text("⏰ Trust request from " + requester.getName() + " has expired.")
                            .color(NamedTextColor.GRAY));
                }
                if (requester.isOnline()) {
                    requester.sendMessage(Component.text("⏰ Your trust request to " + target.getName() + " has expired.")
                            .color(NamedTextColor.GRAY));
                }
            }
        }, 1200L); // 60 seconds
    }

    /**
     * Accept a trust request
     */
    public void acceptTrustRequest(Player accepter) {
        UUID requesterUUID = pendingTrustRequests.get(accepter.getUniqueId());
        if (requesterUUID == null) {
            accepter.sendMessage(Component.text("❌ You don't have any pending trust requests!")
                    .color(NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null) {
            accepter.sendMessage(Component.text("❌ The player who sent the trust request is no longer online!")
                    .color(NamedTextColor.RED));
            pendingTrustRequests.remove(accepter.getUniqueId());
            return;
        }

        // Remove the pending request
        pendingTrustRequests.remove(accepter.getUniqueId());

        // Add mutual trust
        addTrust(requester.getUniqueId(), accepter.getUniqueId());
        addTrust(accepter.getUniqueId(), requester.getUniqueId());

        // Save data
        saveTrustData();

        // Send success messages
        requester.sendMessage(Component.text("✅ " + accepter.getName() + " accepted your trust request! You are now allies!")
                .color(NamedTextColor.GREEN));
        accepter.sendMessage(Component.text("✅ You accepted " + requester.getName() + "'s trust request! You are now allies!")
                .color(NamedTextColor.GREEN));
    }

    /**
     * Deny a trust request
     */
    public void denyTrustRequest(Player denier) {
        UUID requesterUUID = pendingTrustRequests.get(denier.getUniqueId());
        if (requesterUUID == null) {
            denier.sendMessage(Component.text("❌ You don't have any pending trust requests!")
                    .color(NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        pendingTrustRequests.remove(denier.getUniqueId());

        denier.sendMessage(Component.text("❌ Trust request denied.")
                .color(NamedTextColor.RED));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text("❌ " + denier.getName() + " denied your trust request.")
                    .color(NamedTextColor.RED));
        }
    }

    /**
     * Remove trust between two players
     */
    public void removeTrust(Player remover, Player target) {
        if (!isTrusted(remover, target)) {
            remover.sendMessage(Component.text("❌ You don't trust " + target.getName() + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Remove mutual trust
        removeTrust(remover.getUniqueId(), target.getUniqueId());
        removeTrust(target.getUniqueId(), remover.getUniqueId());

        saveTrustData();

        remover.sendMessage(Component.text("💔 You no longer trust " + target.getName() + ".")
                .color(NamedTextColor.YELLOW));

        if (target.isOnline()) {
            target.sendMessage(Component.text("💔 " + remover.getName() + " no longer trusts you.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Check if two players trust each other
     */
    public boolean isTrusted(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;
        return isTrusted(player1.getUniqueId(), player2.getUniqueId()) &&
                isTrusted(player2.getUniqueId(), player1.getUniqueId());
    }

    /**
     * Get list of trusted players for a player
     */
    public List<String> getTrustedPlayers(Player player) {
        Set<UUID> trusted = trustRelations.get(player.getUniqueId());
        List<String> names = new ArrayList<>();

        if (trusted != null) {
            for (UUID uuid : trusted) {
                Player trustedPlayer = Bukkit.getPlayer(uuid);
                if (trustedPlayer != null) {
                    names.add(trustedPlayer.getName());
                } else {
                    // Try to get offline player name
                    names.add(Bukkit.getOfflinePlayer(uuid).getName());
                }
            }
        }

        return names;
    }

    // Private helper methods

    private boolean isTrusted(UUID player1, UUID player2) {
        Set<UUID> trusted = trustRelations.get(player1);
        return trusted != null && trusted.contains(player2);
    }

    private void addTrust(UUID player1, UUID player2) {
        trustRelations.computeIfAbsent(player1, k -> new HashSet<>()).add(player2);
    }

    private void removeTrust(UUID player1, UUID player2) {
        Set<UUID> trusted = trustRelations.get(player1);
        if (trusted != null) {
            trusted.remove(player2);
            if (trusted.isEmpty()) {
                trustRelations.remove(player1);
            }
        }
    }

    private void loadTrustData() {
        try {
            if (dataFile.exists()) {
                for (String uuidString : dataConfig.getKeys(false)) {
                    try {
                        UUID playerUUID = UUID.fromString(uuidString);
                        List<String> trustedList = dataConfig.getStringList(uuidString);
                        Set<UUID> trustedSet = new HashSet<>();

                        for (String trustedUuidString : trustedList) {
                            try {
                                trustedSet.add(UUID.fromString(trustedUuidString));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid trusted UUID: " + trustedUuidString);
                            }
                        }

                        if (!trustedSet.isEmpty()) {
                            trustRelations.put(playerUUID, trustedSet);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid player UUID in trust.yml: " + uuidString);
                    }
                }
                plugin.getLogger().info("Loaded " + trustRelations.size() + " trust relationships from file.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading trust data: " + e.getMessage());
        }
    }

    private void saveTrustData() {
        try {
            // Clear existing data
            for (String key : dataConfig.getKeys(false)) {
                dataConfig.set(key, null);
            }

            // Save current data
            for (Map.Entry<UUID, Set<UUID>> entry : trustRelations.entrySet()) {
                List<String> trustedList = new ArrayList<>();
                for (UUID trustedUUID : entry.getValue()) {
                    trustedList.add(trustedUUID.toString());
                }
                dataConfig.set(entry.getKey().toString(), trustedList);
            }

            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trust data: " + e.getMessage());
        }
    }

    public void saveAllData() {
        saveTrustData();
        plugin.getLogger().info("Saved all trust data to file.");
    }
}