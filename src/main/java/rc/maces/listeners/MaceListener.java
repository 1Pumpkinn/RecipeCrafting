package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
import rc.maces.managers.ElementManager;
import rc.maces.managers.MaceManager;

public class MaceListener implements Listener {

    private final MaceManager maceManager;
    private final ElementManager elementManager;

    public MaceListener(MaceManager maceManager, ElementManager elementManager) {
        this.maceManager = maceManager;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (maceManager.isAirMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.WIND_SHOT);
        } else if (maceManager.isFireMace(item)) {
            maceManager.getAbilityManager().executeAbility(player, AbilityManager.OBSIDIAN_CREATION);
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
                maceManager.getAbilityManager().executeAbility(player, AbilityManager.VINE_PULL);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack weapon = attacker.getInventory().getItemInMainHand();

            // Air Mace or Air element: Apply slow falling on hit to ALL living entities
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity victim = (LivingEntity) event.getEntity();

                if (maceManager.isAirMace(weapon) || "AIR".equals(elementManager.getPlayerElement(attacker))) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0)); // 2 seconds
                }
            }

            // Fire Mace or Fire element: Ignite on hit (ALL living entities)
            if (maceManager.isFireMace(weapon) || "FIRE".equals(elementManager.getPlayerElement(attacker))) {
                event.getEntity().setFireTicks(100); // Ignite victim
            }
        }

        // Handle golem damage to prevent attacking summoner
        if (event.getEntity() instanceof IronGolem) {
            IronGolem golem = (IronGolem) event.getEntity();
            BuddyUpAbility.handleGolemDamage(event, golem);
        }

        // Handle golem protection for any living entity attacking a player
        if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
            Player victim = (Player) event.getEntity();
            LivingEntity damager = (LivingEntity) event.getDamager();

            // Don't trigger if the victim is attacking their own golem
            if (!(damager instanceof IronGolem &&
                    damager.getCustomName() != null &&
                    damager.getCustomName().contains(victim.getName()))) {
                BuddyUpAbility.handlePlayerDamage(event, victim);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Handle golem death cleanup
        if (event.getEntity() instanceof IronGolem) {
            IronGolem golem = (IronGolem) event.getEntity();
            BuddyUpAbility.handleGolemDeath(golem);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String playerElement = elementManager.getPlayerElement(player);

        // Air Mace: Fall damage immunity (holding mace or air element role)
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                (maceManager.isAirMace(mainHand) || maceManager.isAirMace(offHand) || "AIR".equals(playerElement))) {
            event.setCancelled(true);
        }

        // Fire Mace: Fire immunity (only when holding mace)
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

        // Wind Charge pulling effect - works on ALL living entities (with mace or air element role)
        if (projectile instanceof WindCharge && event.getHitEntity() instanceof LivingEntity) {
            LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player) {
                Player shooterPlayer = (Player) shooter;

                // Check if shooter has air mace in main hand or offhand, or has air element
                ItemStack mainHand = shooterPlayer.getInventory().getItemInMainHand();
                ItemStack offHand = shooterPlayer.getInventory().getItemInOffHand();
                String shooterElement = elementManager.getPlayerElement(shooterPlayer);

                if (maceManager.isAirMace(mainHand) || maceManager.isAirMace(offHand) || "AIR".equals(shooterElement)) {
                    // Enhanced pulling effect for air mace/element - pull entities into the air towards the player
                    Vector direction = shooterPlayer.getLocation().toVector()
                            .subtract(hitEntity.getLocation().toVector())
                            .normalize()
                            .multiply(2.5); // Strong pull force

                    // Strong upward component to pull entities into the air
                    direction.setY(1.2); // Pull entities up into the air

                    hitEntity.setVelocity(direction);
                    
                    // Add visual effects for the pull
                    hitEntity.getWorld().spawnParticle(Particle.CLOUD, hitEntity.getLocation(), 15);
                    hitEntity.getWorld().spawnParticle(Particle.SMOKE, hitEntity.getLocation(), 10);
                    
                    // Send message to players
                    if (hitEntity instanceof Player) {
                        ((Player) hitEntity).sendMessage(Component.text("💨 Pulled into the air by wind charge!")
                                .color(NamedTextColor.GRAY));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Earth Mace: All food acts like golden apples (only when holding mace)
        if (maceManager.isEarthMace(mainHand) || maceManager.isEarthMace(offHand)) {
            // Apply golden apple effects (Regeneration II for 5 seconds, Absorption for 2 minutes)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        }
    }
}