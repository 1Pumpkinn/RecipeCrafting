package rc.maces.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rc.maces.managers.CombatTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command to manage safe zones where combat cannot occur
 * Usage: /safezone <pos1|pos2|create|remove|list|info|tp> [args...]
 * Uses WorldEdit-style position selection system
 */
public class SafeZoneCommand implements CommandExecutor, TabCompleter {

    private final CombatTimer combatTimer;

    // Store player selections (pos1 and pos2)
    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    public SafeZoneCommand(CombatTimer combatTimer) {
        this.combatTimer = combatTimer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "pos1":
            case "p1":
            case "1":
                return handlePos1(player);

            case "pos2":
            case "p2":
            case "2":
                return handlePos2(player);

            case "create":
            case "add":
            case "new":
                return handleCreate(player, args);

            case "remove":
            case "delete":
            case "rem":
                return handleRemove(player, args);

            case "list":
            case "ls":
                return handleList(player);

            case "info":
            case "check":
                return handleInfo(player, args);

            case "tp":
            case "teleport":
                return handleTeleport(player, args);

            case "clear":
            case "reset":
                return handleClear(player);

            case "reload":
                return handleReload(player);

            default:
                player.sendMessage(Component.text("❌ Unknown subcommand: " + subCommand)
                        .color(NamedTextColor.RED));
                showHelp(player);
                return true;
        }
    }

    /**
     * Set position 1 at current location
     */
    private boolean handlePos1(Player player) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to set safe zone positions!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Location loc = player.getLocation();
        pos1Selections.put(player.getUniqueId(), loc.clone());

        player.sendMessage(Component.text("✅ Position 1 set!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("📍 Pos1: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("🌍 World: " + loc.getWorld().getName())
                .color(NamedTextColor.GRAY));

        // Check if we have pos2 and show selection info
        Location pos2 = pos2Selections.get(player.getUniqueId());
        if (pos2 != null && pos2.getWorld().equals(loc.getWorld())) {
            showSelectionInfo(player);
        } else {
            player.sendMessage(Component.text("💡 Now set position 2 with '/safezone pos2'")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }

    /**
     * Set position 2 at current location
     */
    private boolean handlePos2(Player player) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to set safe zone positions!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Location loc = player.getLocation();
        pos2Selections.put(player.getUniqueId(), loc.clone());

        player.sendMessage(Component.text("✅ Position 2 set!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("📍 Pos2: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("🌍 World: " + loc.getWorld().getName())
                .color(NamedTextColor.GRAY));

        // Check if we have pos1 and show selection info
        Location pos1 = pos1Selections.get(player.getUniqueId());
        if (pos1 != null && pos1.getWorld().equals(loc.getWorld())) {
            showSelectionInfo(player);
        } else {
            player.sendMessage(Component.text("💡 Now set position 1 with '/safezone pos1'")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }

    /**
     * Show current selection info
     */
    private void showSelectionInfo(Player player) {
        Location pos1 = pos1Selections.get(player.getUniqueId());
        Location pos2 = pos2Selections.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(Component.text("⚠ Positions are in different worlds!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Calculate selection bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        int totalBlocks = sizeX * sizeY * sizeZ;

        player.sendMessage(Component.text("═══════ SELECTION INFO ═══════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("📐 Size: " + sizeX + " × " + sizeY + " × " + sizeZ + " (" + totalBlocks + " blocks)")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📍 From: " + minX + ", " + minY + ", " + minZ)
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("📍 To: " + maxX + ", " + maxY + ", " + maxZ)
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("🌍 World: " + pos1.getWorld().getName())
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("✅ Ready to create! Use '/safezone create <name>'")
                .color(NamedTextColor.GREEN));
    }

    /**
     * Create a new safe zone using selected positions
     * /safezone create <name>
     */
    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to create safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("❌ Usage: /safezone create <name>")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("💡 First set pos1 and pos2, then create!")
                    .color(NamedTextColor.GRAY));
            return true;
        }

        String name = args[1];
        Location pos1 = pos1Selections.get(player.getUniqueId());
        Location pos2 = pos2Selections.get(player.getUniqueId());

        // Validate positions
        if (pos1 == null || pos2 == null) {
            player.sendMessage(Component.text("❌ You must set both pos1 and pos2 first!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("💡 Use '/safezone pos1' and '/safezone pos2'")
                    .color(NamedTextColor.GRAY));
            return true;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(Component.text("❌ Both positions must be in the same world!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName = pos1.getWorld().getName();

        // Check if safe zone already exists for this world
        if (combatTimer.getSafeZoneForWorld(worldName) != null) {
            player.sendMessage(Component.text("❌ A safe zone already exists in this world!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use '/safezone remove' first, then create a new one.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        // Calculate bounds
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Validate size (prevent massive safe zones that could lag server)
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        long totalBlocks = (long)sizeX * sizeY * sizeZ;

        if (totalBlocks > 10_000_000) { // 10 million blocks max
            player.sendMessage(Component.text("❌ Safe zone is too large! Maximum 10 million blocks.")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Current size: " + totalBlocks + " blocks")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        // Create the safe zone
        combatTimer.addSafeZone(worldName, minX, minY, minZ, maxX, maxY, maxZ, name, player);

        // Send success message
        player.sendMessage(Component.text("═══════════════════════════════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("✅ SAFE ZONE CREATED!")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════════════════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("🏠 Name: " + name)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("🌍 World: " + worldName)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📍 From: " + minX + ", " + minY + ", " + minZ)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📍 To: " + maxX + ", " + maxY + ", " + maxZ)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📐 Size: " + sizeX + " × " + sizeY + " × " + sizeZ + " (" + totalBlocks + " blocks)")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("⚔ Combat is now disabled in this area!")
                .color(NamedTextColor.GREEN));

        // Clear selections after successful creation
        pos1Selections.remove(player.getUniqueId());
        pos2Selections.remove(player.getUniqueId());

        return true;
    }

    /**
     * Remove a safe zone
     * /safezone remove [world]
     */
    private boolean handleRemove(Player player, String[] args) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to remove safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName;
        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = player.getWorld().getName();
        }

        // Check if safe zone exists
        var existingSafeZone = combatTimer.getSafeZoneForWorld(worldName);
        if (existingSafeZone == null) {
            player.sendMessage(Component.text("❌ No safe zone found in world: " + worldName)
                    .color(NamedTextColor.RED));
            return true;
        }

        String zoneName = existingSafeZone.getName();
        combatTimer.removeSafeZone(worldName);

        player.sendMessage(Component.text("✅ Safe zone '" + zoneName + "' removed from world: " + worldName)
                .color(NamedTextColor.GREEN));

        return true;
    }

    /**
     * List all safe zones
     */
    private boolean handleList(Player player) {
        var safeZones = combatTimer.getSafeZones();

        if (safeZones.isEmpty()) {
            player.sendMessage(Component.text("📋 No safe zones are currently configured.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        player.sendMessage(Component.text("═══════ SAFE ZONES ═══════")
                .color(NamedTextColor.GOLD));

        int count = 1;
        for (var entry : safeZones.entrySet()) {
            String worldName = entry.getKey();
            var zone = entry.getValue();

            player.sendMessage(Component.text(count + ". " + zone.getName())
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("   🌍 World: " + worldName)
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("   📍 From: " + zone.getMinX() + ", " + zone.getMinY() + ", " + zone.getMinZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("   📍 To: " + zone.getMaxX() + ", " + zone.getMaxY() + ", " + zone.getMaxZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("   📐 Size: " + zone.getSizeX() + " × " + zone.getSizeY() + " × " + zone.getSizeZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("   👤 Created by: " + zone.getCreatedBy())
                    .color(NamedTextColor.GRAY));

            if (count < safeZones.size()) {
                player.sendMessage(Component.text(""));
            }
            count++;
        }

        player.sendMessage(Component.text("═══════════════════════════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Total: " + safeZones.size() + " safe zones")
                .color(NamedTextColor.GRAY));

        return true;
    }

    /**
     * Show info about current location or selections
     */
    private boolean handleInfo(Player player, String[] args) {
        Location location = player.getLocation();
        var currentZone = combatTimer.getSafeZone(location);

        player.sendMessage(Component.text("═══════ LOCATION INFO ═══════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("🌍 World: " + location.getWorld().getName())
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📍 Your Location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ())
                .color(NamedTextColor.YELLOW));

        if (currentZone != null) {
            player.sendMessage(Component.text("🏠 Safe Zone: " + currentZone.getName())
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("📍 Zone From: " + currentZone.getMinX() + ", " + currentZone.getMinY() + ", " + currentZone.getMinZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("📍 Zone To: " + currentZone.getMaxX() + ", " + currentZone.getMaxY() + ", " + currentZone.getMaxZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("📐 Zone Size: " + currentZone.getSizeX() + " × " + currentZone.getSizeY() + " × " + currentZone.getSizeZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("👤 Created by: " + currentZone.getCreatedBy())
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("⚔ Combat Status: PROTECTED")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
        } else {
            player.sendMessage(Component.text("🏠 Safe Zone: None")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("⚔ Combat Status: PVP ZONE")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
        }

        // Show current selections if admin
        if (player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("📋 Your Selections:")
                    .color(NamedTextColor.AQUA));

            Location pos1 = pos1Selections.get(player.getUniqueId());
            Location pos2 = pos2Selections.get(player.getUniqueId());

            if (pos1 != null) {
                player.sendMessage(Component.text("   Pos1: " + pos1.getBlockX() + ", " + pos1.getBlockY() + ", " + pos1.getBlockZ())
                        .color(NamedTextColor.WHITE));
            } else {
                player.sendMessage(Component.text("   Pos1: Not set")
                        .color(NamedTextColor.GRAY));
            }

            if (pos2 != null) {
                player.sendMessage(Component.text("   Pos2: " + pos2.getBlockX() + ", " + pos2.getBlockY() + ", " + pos2.getBlockZ())
                        .color(NamedTextColor.WHITE));
            } else {
                player.sendMessage(Component.text("   Pos2: Not set")
                        .color(NamedTextColor.GRAY));
            }

            if (pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld())) {
                player.sendMessage(Component.text("   Status: Ready to create!")
                        .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("   Status: Need both positions")
                        .color(NamedTextColor.YELLOW));
            }
        }

        player.sendMessage(Component.text("═══════════════════════════")
                .color(NamedTextColor.GOLD));

        return true;
    }

    /**
     * Teleport to safe zone center (if admin)
     */
    private boolean handleTeleport(Player player, String[] args) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to teleport to safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName;
        if (args.length >= 2) {
            worldName = args[1];
        } else {
            worldName = player.getWorld().getName();
        }

        var zone = combatTimer.getSafeZoneForWorld(worldName);
        if (zone == null) {
            player.sendMessage(Component.text("❌ No safe zone found in world: " + worldName)
                    .color(NamedTextColor.RED));
            return true;
        }

        // Get the world and create location at center of safe zone
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Component.text("❌ World not found: " + worldName)
                    .color(NamedTextColor.RED));
            return true;
        }

        // Calculate center coordinates
        int centerX = (zone.getMinX() + zone.getMaxX()) / 2;
        int centerY = (zone.getMinY() + zone.getMaxY()) / 2;
        int centerZ = (zone.getMinZ() + zone.getMaxZ()) / 2;

        Location teleportLoc = new Location(world, centerX + 0.5, centerY, centerZ + 0.5);

        // Make sure it's a safe location (not inside blocks)
        while (world.getBlockAt(teleportLoc).getType().isSolid() && centerY < zone.getMaxY()) {
            centerY++;
            teleportLoc.setY(centerY);
        }

        // Teleport player
        player.teleport(teleportLoc);
        player.sendMessage(Component.text("✅ Teleported to " + zone.getName() + " center!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("📍 Center: " + centerX + ", " + centerY + ", " + centerZ)
                .color(NamedTextColor.GRAY));

        return true;
    }

    /**
     * Clear current selections
     */
    private boolean handleClear(Player player) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to clear selections!")
                    .color(NamedTextColor.RED));
            return true;
        }

        boolean hadSelections = pos1Selections.remove(player.getUniqueId()) != null ||
                pos2Selections.remove(player.getUniqueId()) != null;

        if (hadSelections) {
            player.sendMessage(Component.text("✅ Position selections cleared!")
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("💡 No selections to clear.")
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }

    /**
     * Reload safe zone configuration
     */
    private boolean handleReload(Player player) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to reload safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        var safeZones = combatTimer.getSafeZones();
        player.sendMessage(Component.text("✅ Safe zone system status checked!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Currently loaded: " + safeZones.size() + " safe zones")
                .color(NamedTextColor.YELLOW));

        return true;
    }

    /**
     * Show help information
     */
    private void showHelp(Player player) {
        player.sendMessage(Component.text("═══════ SAFE ZONE COMMANDS ═══════")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("🏠 Safe zones prevent all combat and PvP!")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📐 Uses WorldEdit-style position selection!")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text(""));

        if (player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("👑 Admin Commands:")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("• /safezone pos1")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Set position 1 at your location")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone pos2")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Set position 2 at your location")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone create <name>")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Create safe zone from pos1 to pos2")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone remove [world]")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Remove safe zone from world")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone clear")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Clear your position selections")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone tp [world]")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Teleport to safe zone center")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
        }

        player.sendMessage(Component.text("📋 Info Commands:")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("• /safezone list")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  List all safe zones")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /safezone info")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  Check current location and selections")
                .color(NamedTextColor.GRAY));

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("💡 Quick Setup Guide:")
                .color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("  1. Go to first corner → /safezone pos1")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  2. Go to opposite corner → /safezone pos2")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  3. Create the zone → /safezone create MyBase")
                .color(NamedTextColor.GRAY));

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("🛡 Safe zones protect against:")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  • All PvP damage in the selected area")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • Mace abilities")
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • Combat timer activation")
                .color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = Arrays.asList("pos1", "pos2", "create", "remove", "list", "info", "clear");
            if (sender.hasPermission("maces.admin")) {
                subCommands = Arrays.asList("pos1", "pos2", "create", "remove", "list", "info", "clear", "tp", "reload");
            }

            for (String subCmd : subCommands) {
                if (subCmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        }
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("remove") || subCommand.equals("tp")) {
                // Add world name completions
                var safeZones = combatTimer.getSafeZones();
                for (String worldName : safeZones.keySet()) {
                    if (worldName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            }
            else if (subCommand.equals("create")) {
                completions.add("<name>");
            }
        }

        return completions;
    }
}