package rc.maces.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        loadTrustData();
    }

    /**
     * Sends a trust request to another player with clickable chat
     */
    public void sendTrustRequest(Player requester, Player target) {
        // Validation checks
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

        // Check if there's already a pending request FROM this requester TO target
        if (pendingTrustRequests.containsKey(target.getUniqueId()) &&
                pendingTrustRequests.get(target.getUniqueId()).equals(requester.getUniqueId())) {
            requester.sendMessage(Component.text("❌ You already have a pending trust request to " + target.getName() + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Check if target already has a pending request FROM someone else
        if (pendingTrustRequests.containsKey(target.getUniqueId())) {
            UUID existingRequesterUUID = pendingTrustRequests.get(target.getUniqueId());
            Player existingRequester = Bukkit.getPlayer(existingRequesterUUID);
            String existingRequesterName = existingRequester != null ? existingRequester.getName() : "Unknown";

            requester.sendMessage(Component.text("❌ " + target.getName() + " already has a pending request from " + existingRequesterName + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Store the pending request
        pendingTrustRequests.put(target.getUniqueId(), requester.getUniqueId());

        // Send confirmation to requester
        requester.sendMessage(Component.text("🤝 Trust request sent to " + target.getName() + "!")
                .color(NamedTextColor.GREEN));

        // Create clickable accept and deny buttons
        Component acceptButton = Component.text("[ACCEPT]")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/trustaccept"))
                .hoverEvent(Component.text("Click to accept the trust request").color(NamedTextColor.GREEN));

        Component denyButton = Component.text("[DENY]")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/trustdeny"))
                .hoverEvent(Component.text("Click to deny the trust request").color(NamedTextColor.RED));

        // Send the trust request message with clickable buttons
        target.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        target.sendMessage(Component.text("🤝 TRUST REQUEST 🤝")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        target.sendMessage(Component.text("═══════════════════════════════════")
                .color(NamedTextColor.GOLD));
        target.sendMessage(Component.text(requester.getName() + " wants to become allies!")
                .color(NamedTextColor.YELLOW));
        target.sendMessage(Component.text(""));
        target.sendMessage(Component.text("Allied players are protected from:")
                .color(NamedTextColor.GRAY));
        target.sendMessage(Component.text("• PvP damage from each other")
                .color(NamedTextColor.DARK_GRAY));
        target.sendMessage(Component.text("• All mace abilities and effects")
                .color(NamedTextColor.DARK_GRAY));
        target.sendMessage(Component.text("• All passive element abilities")
                .color(NamedTextColor.DARK_GRAY));
        target.sendMessage(Component.text(""));
        target.sendMessage(acceptButton.append(Component.text("  ").color(NamedTextColor.WHITE)).append(denyButton));
        target.sendMessage(Component.text(""));
        target.sendMessage(Component.text("Or type: /trustaccept or /trustdeny")
                .color(NamedTextColor.GRAY));
        target.sendMessage(Component.text("Request expires in 60 seconds")
                .color(NamedTextColor.DARK_GRAY));
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
        if (requester == null || !requester.isOnline()) {
            accepter.sendMessage(Component.text("❌ The player who sent the trust request is no longer online!")
                    .color(NamedTextColor.RED));
            pendingTrustRequests.remove(accepter.getUniqueId());
            return;
        }

        // Remove the pending request
        pendingTrustRequests.remove(accepter.getUniqueId());

        // Add mutual trust with validation
        boolean success1 = addTrust(requester.getUniqueId(), accepter.getUniqueId());
        boolean success2 = addTrust(accepter.getUniqueId(), requester.getUniqueId());

        if (!success1 || !success2) {
            plugin.getLogger().warning("Failed to establish trust between " + requester.getName() + " and " + accepter.getName());
        }

        // Save data immediately
        saveTrustData();

        // Verify the trust was established
        if (isTrusted(requester, accepter)) {
            // Send success messages
            Component allyMessage = Component.text("✅ ALLIANCE FORMED! ✅")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true);

            requester.sendMessage(allyMessage);
            requester.sendMessage(Component.text("🤝 " + accepter.getName() + " accepted your trust request!")
                    .color(NamedTextColor.GREEN));
            requester.sendMessage(Component.text("You are now allies and protected from each other's abilities!")
                    .color(NamedTextColor.YELLOW));

            accepter.sendMessage(allyMessage);
            accepter.sendMessage(Component.text("🤝 You accepted " + requester.getName() + "'s trust request!")
                    .color(NamedTextColor.GREEN));
            accepter.sendMessage(Component.text("You are now allies and protected from each other's abilities!")
                    .color(NamedTextColor.YELLOW));

            plugin.getLogger().info("Alliance formed between " + requester.getName() + " and " + accepter.getName());
        } else {
            // Something went wrong
            requester.sendMessage(Component.text("❌ Failed to establish alliance. Please try again.")
                    .color(NamedTextColor.RED));
            accepter.sendMessage(Component.text("❌ Failed to establish alliance. Please try again.")
                    .color(NamedTextColor.RED));
            plugin.getLogger().warning("Trust verification failed between " + requester.getName() + " and " + accepter.getName());
        }
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
     * Remove trust between two players (allows either player to break the alliance)
     */
    public void removeTrust(Player remover, Player target) {
        if (!isTrusted(remover, target)) {
            remover.sendMessage(Component.text("❌ You are not allied with " + target.getName() + "!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Remove mutual trust
        removeTrustRelation(remover.getUniqueId(), target.getUniqueId());
        removeTrustRelation(target.getUniqueId(), remover.getUniqueId());

        saveTrustData();

        remover.sendMessage(Component.text("💔 Alliance with " + target.getName() + " has been broken.")
                .color(NamedTextColor.YELLOW));

        if (target.isOnline()) {
            target.sendMessage(Component.text("💔 " + remover.getName() + " has broken your alliance.")
                    .color(NamedTextColor.YELLOW));
        }

        plugin.getLogger().info("Alliance broken between " + remover.getName() + " and " + target.getName());
    }

    /**
     * Check if two players trust each other (mutual trust required)
     * This is the main method used by abilities to check protection
     */
    public boolean isTrusted(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;
        if (player1.equals(player2)) return false; // Players can't be allied with themselves

        boolean trust1to2 = isTrusted(player1.getUniqueId(), player2.getUniqueId());
        boolean trust2to1 = isTrusted(player2.getUniqueId(), player1.getUniqueId());

        // Debug logging for trust issues
        if (trust1to2 != trust2to1) {
            plugin.getLogger().warning("Asymmetric trust detected between " + player1.getName() + " and " + player2.getName()
                    + " (" + trust1to2 + " vs " + trust2to1 + ")");
        }

        return trust1to2 && trust2to1;
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
                    // Verify mutual trust
                    if (isTrusted(player, trustedPlayer)) {
                        names.add(trustedPlayer.getName());
                    }
                } else {
                    // Try to get offline player name
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name != null) {
                        names.add(name + " (offline)");
                    }
                }
            }
        }

        names.sort(String::compareToIgnoreCase);
        return names;
    }

    // Private helper methods

    private boolean isTrusted(UUID player1, UUID player2) {
        Set<UUID> trusted = trustRelations.get(player1);
        return trusted != null && trusted.contains(player2);
    }

    private boolean addTrust(UUID player1, UUID player2) {
        try {
            Set<UUID> trusted = trustRelations.computeIfAbsent(player1, k -> new HashSet<>());
            return trusted.add(player2);
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding trust relationship: " + e.getMessage());
            return false;
        }
    }

    private void removeTrustRelation(UUID player1, UUID player2) {
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
                int loadedRelations = 0;
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
                            loadedRelations++;
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid player UUID in trust.yml: " + uuidString);
                    }
                }
                plugin.getLogger().info("Loaded " + loadedRelations + " trust relationships from file.");
            } else {
                plugin.getLogger().info("No trust data file found, starting fresh.");
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
            int savedRelations = 0;
            for (Map.Entry<UUID, Set<UUID>> entry : trustRelations.entrySet()) {
                List<String> trustedList = new ArrayList<>();
                for (UUID trustedUUID : entry.getValue()) {
                    trustedList.add(trustedUUID.toString());
                }
                dataConfig.set(entry.getKey().toString(), trustedList);
                savedRelations++;
            }

            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved " + savedRelations + " trust relationships to file.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trust data: " + e.getMessage());
        }
    }

    /**
     * Check if a player can use an ability on another player (not trusted allies)
     * @param caster The player casting the ability
     * @param target The target player
     * @return true if the ability can be used, false if they are trusted allies
     */
    public boolean canUseAbilityOn(Player caster, Player target) {
        if (caster == null || target == null) {
            return true; // Allow if either player is null
        }

        if (caster.equals(target)) {
            return false; // Don't allow self-targeting for harmful abilities
        }

        // Return false if they are trusted allies (ability should be blocked)
        // Return true if they are not allies (ability is allowed)
        boolean areTrusted = isTrusted(caster, target);

        // Debug logging
        if (areTrusted) {
            plugin.getLogger().info("Blocking ability from " + caster.getName() + " to " + target.getName() + " (trusted allies)");
        }

        return !areTrusted;
    }

    /**
     * Get the number of pending trust requests (for debugging)
     */
    public int getPendingRequestCount() {
        return pendingTrustRequests.size();
    }

    /**
     * Get the total number of trust relationships (for debugging)
     */
    public int getTotalTrustRelationships() {
        return trustRelations.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Clean up any orphaned trust relationships (where one side trusts but the other doesn't)
     * This can help fix trust system issues
     */
    public void cleanupOrphanedTrusts() {
        Map<UUID, Set<UUID>> toRemove = new HashMap<>();
        int cleanedCount = 0;

        for (Map.Entry<UUID, Set<UUID>> entry : trustRelations.entrySet()) {
            UUID player1 = entry.getKey();
            Set<UUID> trustedByPlayer1 = entry.getValue();

            for (UUID player2 : new HashSet<>(trustedByPlayer1)) {
                // Check if player2 also trusts player1
                Set<UUID> trustedByPlayer2 = trustRelations.get(player2);
                if (trustedByPlayer2 == null || !trustedByPlayer2.contains(player1)) {
                    // Orphaned relationship - remove it
                    toRemove.computeIfAbsent(player1, k -> new HashSet<>()).add(player2);
                    cleanedCount++;
                    plugin.getLogger().info("Cleaning orphaned trust: " + player1 + " -> " + player2);
                }
            }
        }

        // Remove orphaned relationships
        for (Map.Entry<UUID, Set<UUID>> entry : toRemove.entrySet()) {
            Set<UUID> currentTrusted = trustRelations.get(entry.getKey());
            if (currentTrusted != null) {
                currentTrusted.removeAll(entry.getValue());
                if (currentTrusted.isEmpty()) {
                    trustRelations.remove(entry.getKey());
                }
            }
        }

        if (cleanedCount > 0) {
            saveTrustData();
            plugin.getLogger().info("Cleaned up " + cleanedCount + " orphaned trust relationships");
        } else {
            plugin.getLogger().info("No orphaned trust relationships found");
        }
    }

    public void saveAllData() {
        saveTrustData();
        plugin.getLogger().info("Saved all trust data to file.");
    }
}