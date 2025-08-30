package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import rc.maces.managers.CombatTimer;
import rc.maces.managers.TrustManager;

/**
 * Prevents players from using elytras during combat, unless their combat
 * was caused by a trusted ally (preventing abuse while allowing ally interactions)
 */
public class ElytraDisabling implements Listener {

    private final CombatTimer combatTimer;
    private final TrustManager trustManager;
    private final JavaPlugin plugin;

    public ElytraDisabling(CombatTimer combatTimer, TrustManager trustManager, JavaPlugin plugin) {
        this.combatTimer = combatTimer;
        this.trustManager = trustManager;
        this.plugin = plugin;
    }

    /**
     * Prevent elytra gliding during combat (unless caused by ally)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onElytraToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only prevent if trying to start gliding (not stopping)
        if (!event.isGliding()) {
            return;
        }

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow elytra use
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra use in ally-caused combat
        }

        // Block elytra gliding
        event.setCancelled(true);

        // Force stop gliding to make sure
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isGliding()) {
                    player.setGliding(false);
                }
            }
        }.runTaskLater(plugin, 1L);

        sendCombatMessage(player, "You cannot use elytra while in combat!");

        plugin.getLogger().info("Blocked elytra gliding for " + player.getName() + " (in combat)");
    }

    /**
     * Prevent equipping elytra during combat by clicking in inventory
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal inventory actions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        // Check if player is trying to equip elytra
        if (isElytraEquipAttempt(event, player)) {
            event.setCancelled(true);
            sendCombatMessage(player, "You cannot equip elytra while in combat!");
            plugin.getLogger().info("Blocked elytra equipping for " + player.getName() + " (in combat)");
        }
    }

    /**
     * Prevent equipping elytra during combat by dragging in inventory
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal inventory actions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        // Check if dragging elytra to chestplate slot
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && draggedItem.getType() == Material.ELYTRA) {
            // Check if any of the drag slots is the chestplate slot (slot 38 in player inventory)
            if (event.getRawSlots().contains(38)) {
                event.setCancelled(true);
                sendCombatMessage(player, "You cannot equip elytra while in combat!");
                plugin.getLogger().info("Blocked elytra drag equipping for " + player.getName() + " (in combat)");
            }
        }
    }

    /**
     * Prevent right-clicking elytra to equip during combat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal interactions
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow elytra equipping in ally-caused combat
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ELYTRA) {
            // Check if player is trying to right-click equip (and doesn't already have chestplate)
            PlayerInventory inventory = player.getInventory();
            ItemStack chestplate = inventory.getChestplate();

            if (chestplate == null || chestplate.getType() == Material.AIR) {
                event.setCancelled(true);
                sendCombatMessage(player, "You cannot equip elytra while in combat!");
                plugin.getLogger().info("Blocked elytra right-click equipping for " + player.getName() + " (in combat)");
            }
        }
    }

    /**
     * Additional safety: Prevent switching to hotbar slot containing elytra during combat
     * (This is more of a QoL feature to prevent accidental attempts)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!combatTimer.isInCombat(player)) {
            return; // Not in combat, allow normal item switching
        }

        // Check if combat was caused by a trusted ally
        if (combatTimer.isCombatCausedByAlly(player)) {
            return; // Allow normal item switching in ally-caused combat
        }

        // Check if switching to elytra
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && newItem.getType() == Material.ELYTRA) {
            // Don't cancel the event, just give a warning
            player.sendMessage(Component.text("⚠ You cannot equip that elytra while in combat!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Helper method to determine if an inventory click is attempting to equip elytra
     */
    private boolean isElytraEquipAttempt(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        plugin.getLogger().info("Checking elytra equip attempt: slot=" + event.getSlot() + ", slotType=" + event.getSlotType() +
                ", clickedItem=" + (clickedItem != null ? clickedItem.getType() : "null") +
                ", cursorItem=" + (cursorItem != null ? cursorItem.getType() : "null"));

        // Check if clicking on chestplate slot (slot 38)
        if (event.getSlot() == 38) {
            // Placing item from cursor into chestplate slot
            if (cursorItem != null && cursorItem.getType() == Material.ELYTRA) {
                plugin.getLogger().info("Detected elytra placement in chestplate slot");
                return true;
            }
        }

        // Check if shift-clicking elytra to auto-equip
        if (clickedItem != null && clickedItem.getType() == Material.ELYTRA && event.getClick().isShiftClick()) {
            PlayerInventory inventory = player.getInventory();
            ItemStack currentChestplate = inventory.getChestplate();
            if (currentChestplate == null || currentChestplate.getType() == Material.AIR) {
                plugin.getLogger().info("Detected shift-click elytra auto-equip");
                return true;
            }
        }

        // Check for number key equipping (pressing 1-9 while hovering over chestplate slot)
        if (event.getSlot() == 38 && event.getClick().isKeyboardClick()) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0 && hotbarSlot < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                if (hotbarItem != null && hotbarItem.getType() == Material.ELYTRA) {
                    plugin.getLogger().info("Detected number key elytra swap");
                    return true;
                }
            }
        }

        // Check for double-click auto-equip
        if (event.getClick().toString().contains("DOUBLE_CLICK") && cursorItem != null && cursorItem.getType() == Material.ELYTRA) {
            plugin.getLogger().info("Detected double-click elytra equip");
            return true;
        }

        return false;
    }

    /**
     * Send combat message to player
     */
    private void sendCombatMessage(Player player, String message) {
        player.sendMessage(Component.text("❌ " + message)
                .color(NamedTextColor.RED));
        player.sendMessage(Component.text("⚔ Combat ends in: " + combatTimer.getRemainingCombatTimeFormatted(player))
                .color(NamedTextColor.YELLOW));
    }

    /**
     * Check if a player is currently wearing elytra (for other systems to use)
     */
    public boolean isWearingElytra(Player player) {
        if (player == null) return false;

        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }

    /**
     * Force remove elytra from a player (for admin commands or special situations)
     */
    public void forceRemoveElytra(Player player, String reason) {
        if (player == null || !isWearingElytra(player)) {
            return;
        }

        ItemStack elytra = player.getInventory().getChestplate();
        player.getInventory().setChestplate(null);

        // Try to add to inventory, drop if full
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(elytra);
        } else {
            player.getWorld().dropItem(player.getLocation(), elytra);
        }

        // Stop gliding if currently gliding
        if (player.isGliding()) {
            player.setGliding(false);
        }

        if (reason != null && !reason.isEmpty()) {
            player.sendMessage(Component.text("🪂 Your elytra has been removed: " + reason)
                    .color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Continuous check to remove elytras from players in combat (safety net)
     */
    public void startElytraMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (combatTimer.isInCombat(player) && !combatTimer.isCombatCausedByAlly(player)) {
                        // Check if player is wearing elytra
                        if (isWearingElytra(player)) {
                            player.sendMessage(Component.text("⚠ Elytra automatically removed due to combat!")
                                    .color(NamedTextColor.YELLOW));
                            forceRemoveElytra(player, "entered combat");
                        }

                        // Check if player is gliding
                        if (player.isGliding()) {
                            player.setGliding(false);
                            player.sendMessage(Component.text("⚠ Elytra flight stopped due to combat!")
                                    .color(NamedTextColor.YELLOW));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Check every half second
    }

    /**
     * Get combat and trust managers (for integration with other systems)
     */
    public CombatTimer getCombatTimer() {
        return combatTimer;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }
}