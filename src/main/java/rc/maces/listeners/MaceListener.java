package rc.maces.listeners;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import rc.maces.abilities.AbilityManager;
import rc.maces.abilities.earth.BuddyUpAbility;
import rc.maces.abilities.fire.FirePassthroughAbility;
import rc.maces.managers.MaceManager;

public class MaceListener implements Listener {

    private final MaceManager maceManager;

    public MaceListener(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (maceManager.isAirMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.WIND_SHOT);
        } else if (maceManager.isFireMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.FIRE_PASSTHROUGH);
        } else if (maceManager.isWaterMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.WATER_HEAL);
        } else if (maceManager.isEarthMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.BUDDY_UP);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (maceManager.isCustomMace(item)) {
            event.setCancelled(true);

            if (maceManager.isAirMace(item)) {
                maceManager.getAbilityManager().executeAbility(player, AbilityManager.WIND_STRUCK);
            } else if (maceManager.isFireMace(item)) {
                maceManager.getAbilityManager().executeAbility(player, AbilityManager.METEORS);
            } else if (maceManager.isWaterMace(item)) {
                maceManager.getAbilityManager().executeAbility(player, AbilityManager.WATER_GEYSER);
            } else if (maceManager.isEarthMace(item)) {
                maceManager.getAbilityManager().executeAbility(player, AbilityManager.TORNADO);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack weapon = attacker.getInventory().getItemInMainHand();

            // Air Mace: Apply slow falling on hit to all living entities
            if (maceManager.isAirMace(weapon) && event.getEntity() instanceof LivingEntity) {
                LivingEntity victim = (LivingEntity) event.getEntity();
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0)); // 2 seconds
            }

            // Fire Mace: Ignite on hit + bonus damage when on fire
            else if (maceManager.isFireMace(weapon)) {
                event.getEntity().setFireTicks(100);

                // Bonus damage when attacker is on fire
                if (attacker.getFireTicks() > 0) {
                    event.setDamage(event.getDamage() + 4.0); // +2 hearts
                }

                // Handle fire passthrough true damage for all living entities
                if (FirePassthroughAbility.hasFirePassthrough(attacker) &&
                        event.getEntity() instanceof LivingEntity) {
                    LivingEntity victim = (LivingEntity) event.getEntity();
                    // Convert to true damage
                    event.setCancelled(true);
                    double newHealth = Math.max(0, victim.getHealth() - event.getFinalDamage());
                    victim.setHealth(newHealth);
                }
            }

            // Earth Mace: Trigger golem protection when player is attacked
            else if (maceManager.isEarthMace(weapon) && event.getEntity() instanceof Player) {
                BuddyUpAbility.handlePlayerDamage(event, (Player) event.getEntity());
            }
        }

        // Handle golem protection for any living entity attacking a player
        if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
            BuddyUpAbility.handlePlayerDamage(event, (Player) event.getEntity());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Air Mace: Fall damage immunity
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                (maceManager.isAirMace(mainHand) || maceManager.isAirMace(offHand))) {
            event.setCancelled(true);
        }

        // Fire Mace: Fire immunity
        if ((event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) &&
                (maceManager.isFireMace(mainHand) || maceManager.isFireMace(offHand))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // Wind Charge pulling effect - now works on all living entities
        if (projectile instanceof WindCharge && event.getHitEntity() instanceof LivingEntity) {
            LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player) {
                Player shooterPlayer = (Player) shooter;

                // Pull the hit entity towards the shooter
                Vector direction = shooterPlayer.getLocation().toVector()
                        .subtract(hitEntity.getLocation().toVector())
                        .normalize()
                        .multiply(2.0);

                hitEntity.setVelocity(direction);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Earth Mace: All food acts like golden apples
        if (maceManager.isEarthMace(mainHand) || maceManager.isEarthMace(offHand)) {
            // Apply golden apple effects (Regeneration II for 5 seconds, Absorption for 2 minutes)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        }
    }
}