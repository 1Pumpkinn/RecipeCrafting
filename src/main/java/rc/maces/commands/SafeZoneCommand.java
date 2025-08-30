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
import java.util.List;

/**
 * Command to manage safe zones where combat cannot occur
 * Usage: /safezone <create|remove|list|info|tp> [args...]
 */
public class SafeZoneCommand implements CommandExecutor, TabCompleter {

    private final CombatTimer combatTimer;

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

            case "here":
                return handleHere(player, args);

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
     * Create a new safe zone
     * /safezone create <name> [radius] [centerX] [centerZ]
     * /safezone create <name> [radius] (uses current location)
     */
    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to create safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("❌ Usage: /safezone create <name> [radius] [centerX] [centerZ]")
                    .color(NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        int radius = 100; // Default radius
        Location location = player.getLocation();
        int centerX = location.getBlockX();
        int centerZ = location.getBlockZ();

        try {
            // Parse radius if provided
            if (args.length >= 3) {
                radius = Integer.parseInt(args[2]);
                if (radius <= 0) {
                    player.sendMessage(Component.text("❌ Radius must be positive!")
                            .color(NamedTextColor.RED));
                    return true;
                }
                if (radius > 1000) {
                    player.sendMessage(Component.text("❌ Radius cannot exceed 1000 blocks!")
                            .color(NamedTextColor.RED));
                    return true;
                }
            }

            // Parse coordinates if provided
            if (args.length >= 5) {
                centerX = Integer.parseInt(args[3]);
                centerZ = Integer.parseInt(args[4]);
            }

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("❌ Invalid number format!")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName = location.getWorld().getName();

        // Check if safe zone already exists for this world
        if (combatTimer.getSafeZoneName(location) != null) {
            player.sendMessage(Component.text("❌ A safe zone already exists in this world!")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use '/safezone remove' first, then create a new one.")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        // Create the safe zone
        combatTimer.addSafeZone(worldName, centerX, centerZ, radius, name, player);

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
        player.sendMessage(Component.text("📍 Center: " + centerX + ", " + centerZ)
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("📏 Radius: " + radius + " blocks")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("⚔ Combat is now disabled in this area!")
                .color(NamedTextColor.GREEN));

        return true;
    }

    /**
     * Create a safe zone at current location
     * /safezone here <name> [radius]
     */
    private boolean handleHere(Player player, String[] args) {
        if (!player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("❌ You need admin permission to create safe zones!")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("❌ Usage: /safezone here <name> [radius]")
                    .color(NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        int radius = 100; // Default radius

        if (args.length >= 3) {
            try {
                radius = Integer.parseInt(args[2]);
                if (radius <= 0 || radius > 1000) {
                    player.sendMessage(Component.text("❌ Radius must be between 1 and 1000 blocks!")
                            .color(NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid radius number!")
                        .color(NamedTextColor.RED));
                return true;
            }
        }

        Location loc = player.getLocation();
        String worldName = loc.getWorld().getName();

        // Check if safe zone already exists
        if (combatTimer.getSafeZoneName(loc) != null) {
            player.sendMessage(Component.text("❌ A safe zone already exists in this world!")
                    .color(NamedTextColor.RED));
            return true;
        }

        combatTimer.addSafeZone(worldName, loc.getBlockX(), loc.getBlockZ(), radius, name, player);

        player.sendMessage(Component.text("✅ Safe zone '" + name + "' created at your location!")
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("📏 Radius: " + radius + " blocks")
                .color(NamedTextColor.YELLOW));

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
        String existingName = combatTimer.getSafeZoneName(player.getLocation());
        if (existingName == null && args.length < 2) {
            player.sendMessage(Component.text("❌ No safe zone found in current world!")
                    .color(NamedTextColor.RED));
            return true;
        }

        combatTimer.removeSafeZone(worldName);

        player.sendMessage(Component.text("✅ Safe zone removed from world: " + worldName)
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
            player.sendMessage(Component.text("   📍 Center: " + zone.getCenterX() + ", " + zone.getCenterZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("   📏 Radius: " + zone.getRadius() + " blocks")
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
     * Show info about current location or specific safe zone
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
            player.sendMessage(Component.text("📍 Zone Center: " + currentZone.getCenterX() + ", " + currentZone.getCenterZ())
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("📏 Zone Radius: " + currentZone.getRadius() + " blocks")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("👤 Created by: " + currentZone.getCreatedBy())
                    .color(NamedTextColor.GRAY));

            // Calculate distance from center and to edge
            double distanceFromCenter = Math.sqrt(
                    Math.pow(location.getX() - currentZone.getCenterX(), 2) +
                            Math.pow(location.getZ() - currentZone.getCenterZ(), 2)
            );
            int edgeDistance = currentZone.getRadius() - (int)distanceFromCenter;

            player.sendMessage(Component.text("📏 Distance from center: " + (int)distanceFromCenter + " blocks")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("📏 Distance to edge: " + edgeDistance + " blocks")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("⚔ Combat Status: PROTECTED")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true));
        } else {
            player.sendMessage(Component.text("🏠 Safe Zone: None")
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("⚔ Combat Status: PVP ZONE")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));

            // Show distance to nearest safe zone
            double distanceToSafeZone = combatTimer.getDistanceToNearestSafeZone(location);
            if (distanceToSafeZone != Double.MAX_VALUE && distanceToSafeZone > 0) {
                player.sendMessage(Component.text("🛡 Nearest safe zone: " + (int)distanceToSafeZone + " blocks away")
                        .color(NamedTextColor.GRAY));
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

        // Get the world and create location
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Component.text("❌ World not found: " + worldName)
                    .color(NamedTextColor.RED));
            return true;
        }

        // Find a safe Y coordinate at the center
        Location centerLoc = new Location(world, zone.getCenterX() + 0.5, 100, zone.getCenterZ() + 0.5);
        Location safeLoc = world.getHighestBlockAt(centerLoc).getLocation().add(0, 1, 0);

        // Teleport player
        player.teleport(safeLoc);
        player.sendMessage(Component.text("✅ Teleported to " + zone.getName() + " center!")
                .color(NamedTextColor.GREEN));

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

        // This would trigger a reload from the CombatTimer
        // For now, just show current status
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
        player.sendMessage(Component.text(""));

        if (player.hasPermission("maces.admin")) {
            player.sendMessage(Component.text("👑 Admin Commands:")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("• /safezone create <name> [radius] [x] [z]")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Create a safe zone (default radius: 100)")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone here <name> [radius]")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Create a safe zone at your location")
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("• /safezone remove [world]")
                    .color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  Remove safe zone from world")
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
        player.sendMessage(Component.text("  Check current location safety")
                .color(NamedTextColor.GRAY));

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("💡 Safe zones protect against:")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  • All PvP damage")
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
            List<String> subCommands = Arrays.asList("create", "remove", "list", "info", "here");
            if (sender.hasPermission("maces.admin")) {
                subCommands = Arrays.asList("create", "remove", "list", "info", "here", "tp", "reload");
            }

            for (String subCmd : subCommands) {
                if (subCmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        }
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("remove")) {
                // Could add world name completions here
                completions.add("world");
                completions.add("world_nether");
                completions.add("world_the_end");
            }
            else if (subCommand.equals("create") || subCommand.equals("here")) {
                completions.add("<name>");
            }
        }
        else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("create") || subCommand.equals("here")) {
                completions.add("[radius]");
                completions.add("50");
                completions.add("100");
                completions.add("200");
            }
        }

        return completions;
    }
}