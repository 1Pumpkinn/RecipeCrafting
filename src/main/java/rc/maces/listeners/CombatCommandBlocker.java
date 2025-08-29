package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import rc.maces.managers.CombatTimer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Listener that blocks most commands while players are in combat
 * Allows certain essential commands for safety and admin purposes
 */
public class CombatCommandBlocker implements Listener {

    private final CombatTimer combatTimer;

    // Commands that are ALWAYS allowed during combat (safety and essential commands)
    private final Set<String> allowedCommands = new HashSet<>(Arrays.asList(
            // Essential communication
            "msg", "tell", "whisper", "w", "pm", "message",
            "r", "reply",
            "say", "me",

            // Help and information
            "help", "?",
            "list", "who", "online",
            "ping", "tps",
            "rules", "info",

            // Combat-related commands
            "combat", "combattimer", "pvp",

            // Trust system (players should be able to manage allies during combat)
            "trust", "untrust", "trustaccept", "trustdeny", "trustlist",
            "allies", "trustallies", "trustedplayers",

            // Element/mace info (read-only commands)
            "myelement", "myel", "element-info", "elementinfo",
            "craftedmaces", "maces", "maceinv", "maceinventory",

            // Plugin help
            "macehelp", "maceinfo", "rhelp"
    ));

    // Commands that are allowed for OPs/admins even during combat
    private final Set<String> adminAllowedCommands = new HashSet<>(Arrays.asList(
            // Admin commands
            "op", "deop", "kick", "ban", "pardon", "tempban",
            "mute", "unmute", "jail", "unjail",
            "vanish", "v", "invsee", "enderchest",
            "fly", "god", "heal", "feed",
            "gamemode", "gm", "gms", "gmc", "gma", "gmsp",

            // Plugin admin commands
            "element", "el", "elem", "reroll", "elementreroll", "rerollelement",
            "resetmaces", "resetmace", "macereset", "clearmaces",
            "macesecurity", "msecurity", "macesec", "securitycheck",
            "macereload", "rcreload", "reloadmaces",
            "scanmaces", "trustdebug", "tdebug", "trustcheck",
            "macestatus", "mstatus", "servermaces", "globalstatus",

            // Mace giving commands
            "airmace", "firemace", "watermace", "earthmace",

            // Essential admin tools
            "tp", "teleport", "tphere", "tpall",
            "world", "mv", "multiverse",
            "time", "weather", "difficulty",
            "give", "item", "i",
            "clear", "clearinventory", "ci",
            "reload", "rl", "restart", "stop"
    ));

    // Commands that are NEVER allowed during combat (bypass/escape commands)
    private final Set<String> blockedCommands = new HashSet<>(Arrays.asList(
            // Movement/teleportation
            "home", "sethome", "delhome", "homes",
            "spawn", "setspawn",
            "warp", "warps", "setwarp", "delwarp",
            "back", "return",
            "tpa", "tpask", "tpaccept", "tpadeny", "tpahere",
            "rtp", "randomtp", "wild", "wilderness",
            "hub", "lobby",

            // Economy/trading
            "pay", "money", "balance", "bal", "economy", "eco",
            "shop", "buy", "sell", "trade", "auction", "ah",
            "chest", "cshop", "chestshop",

            // Storage/items
            "enderchest", "ec", "echest",
            "backpack", "bp", "bag",
            "vault", "pv", "playervault",

            // Protection/claims
            "claim", "unclaim", "trust", "untrust", "abandonclaim",
            "claimlist", "claiminfo", "claimtrust",
            "res", "residence", "rg", "region", "worldguard",
            "plot", "p", "plots",

            // Misc escape/advantage commands
            "kit", "kits",
            "repair", "fix", "anvil",
            "enchant", "ench",
            "suicide", "kill",
            "afk", "away"
    ));

    public CombatCommandBlocker(CombatTimer combatTimer) {
        this.combatTimer = combatTimer;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Only block commands if player is in combat
        if (!combatTimer.isInCombat(player)) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String command = extractCommand(message);

        // Allow admins to use admin commands during combat
        if (player.isOp() || player.hasPermission("maces.admin")) {
            if (adminAllowedCommands.contains(command)) {
                return; // Allow admin command
            }
        }

        // Check if command is explicitly blocked
        if (blockedCommands.contains(command)) {
            event.setCancelled(true);
            sendCombatBlockMessage(player, command);
            return;
        }

        // Check if command is in allowed list
        if (allowedCommands.contains(command)) {
            return; // Allow the command
        }

        // Check for plugin-specific commands that should be allowed
        if (isPluginAllowedCommand(command)) {
            return; // Allow plugin command
        }

        // Block all other commands
        event.setCancelled(true);
        sendCombatBlockMessage(player, command);
    }

    /**
     * Extract the command name from the full command string
     */
    private String extractCommand(String fullCommand) {
        // Remove the leading slash and get the first word
        String withoutSlash = fullCommand.substring(1);
        int spaceIndex = withoutSlash.indexOf(' ');
        if (spaceIndex == -1) {
            return withoutSlash;
        }
        return withoutSlash.substring(0, spaceIndex);
    }

    /**
     * Check if a command is a plugin-specific command that should be allowed
     */
    private boolean isPluginAllowedCommand(String command) {
        // Allow certain plugin prefixes for essential functionality
        return command.startsWith("mace") && (
                command.equals("macehelp") ||
                        command.equals("maceinfo") ||
                        command.equals("macestatus") ||
                        command.contains("help") ||
                        command.contains("info") ||
                        command.contains("status")
        );
    }

    /**
     * Send a message to the player explaining why their command was blocked
     */
    private void sendCombatBlockMessage(Player player, String command) {
        player.sendMessage(Component.text("❌ You cannot use '/" + command + "' while in combat!")
                .color(NamedTextColor.RED));

        long remainingTime = combatTimer.getRemainingCombatTime(player);
        if (remainingTime > 0) {
            String timeString = combatTimer.getRemainingCombatTimeFormatted(player);
            player.sendMessage(Component.text("⚔ Combat ends in: " + timeString)
                    .color(NamedTextColor.YELLOW));
        }

        // Show helpful message for first-time offenders
        player.sendMessage(Component.text("💡 You can still use chat, help commands, and trust system!")
                .color(NamedTextColor.GRAY));
    }

    /**
     * Add a command to the allowed list (for other plugins to use)
     */
    public void addAllowedCommand(String command) {
        allowedCommands.add(command.toLowerCase());
    }

    /**
     * Remove a command from the allowed list
     */
    public void removeAllowedCommand(String command) {
        allowedCommands.remove(command.toLowerCase());
    }

    /**
     * Add a command to the blocked list (for other plugins to use)
     */
    public void addBlockedCommand(String command) {
        blockedCommands.add(command.toLowerCase());
    }

    /**
     * Remove a command from the blocked list
     */
    public void removeBlockedCommand(String command) {
        blockedCommands.remove(command.toLowerCase());
    }

    /**
     * Check if a command would be blocked for a player in combat
     */
    public boolean isCommandBlocked(String command, Player player) {
        if (!combatTimer.isInCombat(player)) {
            return false;
        }

        command = command.toLowerCase();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Admin bypass
        if ((player.isOp() || player.hasPermission("maces.admin")) && adminAllowedCommands.contains(command)) {
            return false;
        }

        // Explicitly blocked
        if (blockedCommands.contains(command)) {
            return true;
        }

        // Explicitly allowed
        if (allowedCommands.contains(command)) {
            return false;
        }

        // Plugin allowed
        if (isPluginAllowedCommand(command)) {
            return false;
        }

        // Default: block
        return true;
    }

    /**
     * Get the list of allowed commands (for debugging/info)
     */
    public Set<String> getAllowedCommands() {
        return new HashSet<>(allowedCommands);
    }

    /**
     * Get the list of blocked commands (for debugging/info)
     */
    public Set<String> getBlockedCommands() {
        return new HashSet<>(blockedCommands);
    }

    /**
     * Get the list of admin allowed commands (for debugging/info)
     */
    public Set<String> getAdminAllowedCommands() {
        return new HashSet<>(adminAllowedCommands);
    }
}