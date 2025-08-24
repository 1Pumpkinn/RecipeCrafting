package rc.maces.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rc.maces.managers.MaceManager;

import java.util.Collection;
import java.util.Random;
import java.util.UUID;

public class MaceListener implements Listener {

    private final MaceManager maceManager;
    private final Random random = new Random();

    public MaceListener(MaceManager maceManager) {
        this.maceManager = maceManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction().name().contains("RIGHT_CLICK")) {
            handleRightClick(player, item);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (maceManager.isCustomMace(item)) {
            event.setCancelled(true);
            handleOffhandAbility(player, item);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack weapon = attacker.getInventory().getItemInMainHand();

            if (maceManager.isAirMace(weapon) && event.getDamage() > 4) {
                // Apply slow falling for strong hits
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity victim = (LivingEntity) event.getEntity();
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
                }
            } else if (maceManager.isFireMace(weapon)) {
                // Set entity on fire
                event.getEntity().setFireTicks(100);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack mainHand = player.getInventory().getItemInMainHand();

            // Air Mace fall damage immunity
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                    maceManager.isAirMace(mainHand)) {
                event.setCancelled(true);
            }

            // Fire Mace fire immunity
            if ((event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA) &&
                    maceManager.isFireMace(mainHand)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        if (projectile instanceof WindCharge && event.getHitEntity() != null) {
            Entity hitEntity = event.getHitEntity();
            ProjectileSource shooter = projectile.getShooter();

            if (shooter instanceof Player) {
                // Allow wind charges to hit anyone, including the caster
                // Calculate launch direction
                Vector direction = hitEntity.getLocation().toVector()
                        .subtract(((Player) shooter).getLocation().toVector())
                        .normalize()
                        .multiply(3);

                hitEntity.setVelocity(direction);
            }
        } else if (projectile instanceof Fireball && event.getHitEntity() != null) {
            Entity hitEntity = event.getHitEntity();
            hitEntity.setFireTicks(160);

            if (hitEntity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) hitEntity;
                living.damage(3);
            }
        }
    }

    private void handleRightClick(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();

        if (maceManager.isAirMace(item)) {
            // Wind Shot ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "wind_shot")) {
                WindCharge windCharge = player.getWorld().spawn(player.getEyeLocation(), WindCharge.class);
                windCharge.setShooter(player);
                windCharge.setVelocity(player.getEyeLocation().getDirection().multiply(2));

                maceManager.getCooldownManager().setCooldown(playerId, "wind_shot", 5000);
            }
        } else if (maceManager.isFireMace(item)) {
            // Water to Lava ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "water_to_lava")) {
                convertWaterToLava(player);
                maceManager.getCooldownManager().setCooldown(playerId, "water_to_lava", 10000);
            }
        } else if (maceManager.isWaterMace(item)) {
            // Self Heal ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "self_heal")) {
                player.setHealth(Math.min(player.getHealth() + 4, player.getMaxHealth()));
                player.sendMessage(Component.text("♥ Healed +2 hearts!")
                        .color(NamedTextColor.GREEN));

                maceManager.getCooldownManager().setCooldown(playerId, "self_heal", 10000);
            }
        } else if (maceManager.isEarthMace(item)) {
            // Stone Wall ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "stone_wall")) {
                createStoneWall(player);
                maceManager.getCooldownManager().setCooldown(playerId, "stone_wall", 15000);
            }
        }
    }

    private void handleOffhandAbility(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();

        if (maceManager.isAirMace(item)) {
            // Air Burst ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "air_burst")) {
                shootWindChargeCircle(player);
                slowNearbyEnemies(player);
                maceManager.getCooldownManager().setCooldown(playerId, "air_burst", 10000);
            }
        } else if (maceManager.isFireMace(item)) {
            // Meteor Shower ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "meteor_shower")) {
                createMeteorShower(player);
                maceManager.getCooldownManager().setCooldown(playerId, "meteor_shower", 25000);
            }
        } else if (maceManager.isWaterMace(item)) {
            // Water Geyser ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "water_geyser")) {
                createWaterGeyser(player);
                maceManager.getCooldownManager().setCooldown(playerId, "water_geyser", 20000);
            }
        } else if (maceManager.isEarthMace(item)) {
            // Earthquake ability
            if (!maceManager.getCooldownManager().isOnCooldown(playerId, "earthquake")) {
                createEarthquake(player);
                maceManager.getCooldownManager().setCooldown(playerId, "earthquake", 30000);
            }
        }
    }

    private void convertWaterToLava(Player player) {
        Location center = player.getLocation();

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.WATER) {
                        block.setType(Material.LAVA);
                    }
                }
            }
        }

        player.sendMessage(Component.text("🔥 WATER IGNITION! Turned water to lava in a 4x4 area!")
                .color(NamedTextColor.RED));
        player.getWorld().playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 1.0f);
    }

    private void shootWindChargeCircle(Player player) {
        Location center = player.getEyeLocation();

        for (int i = 0; i < 8; i++) {
            double angle = i * 45;
            double radians = Math.toRadians(angle);

            Vector direction = new Vector(Math.cos(radians), 0, Math.sin(radians));

            WindCharge windCharge = player.getWorld().spawn(center, WindCharge.class);
            windCharge.setShooter(player);
            windCharge.setVelocity(direction.multiply(0.5));
        }
    }

    private void slowNearbyEnemies(Player player) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
            }
        }
    }

    private void createMeteorShower(Player player) {
        player.sendMessage(Component.text("☄️ METEOR SHOWER! Raining apocalyptic destruction!")
                .color(NamedTextColor.RED));

        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 4.0f, 0.3f);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.4f);

        new BukkitRunnable() {
            int meteorsLaunched = 0;

            @Override
            public void run() {
                if (meteorsLaunched >= 15) {
                    cancel();
                    return;
                }

                // Random target location within 15x15 area
                int randomX = random.nextInt(15) - 7;
                int randomZ = random.nextInt(15) - 7;

                Location targetLoc = center.clone().add(randomX, 0, randomZ);
                Location meteorLoc = targetLoc.clone().add(0, 20, 0);

                // Create ground warning effects
                targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 20);
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 15);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 10);

                // Launch meteor
                LargeFireball fireball = meteorLoc.getWorld().spawn(meteorLoc, LargeFireball.class);
                fireball.setShooter(player);
                fireball.setDirection(new Vector(0, -1, 0));
                fireball.setVelocity(new Vector(0, -3, 0));

                // Schedule impact effects
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        meteorImpact(targetLoc, player);
                    }
                }.runTaskLater(maceManager.getPlugin(), 14L); // ~0.7 seconds

                meteorsLaunched++;
            }
        }.runTaskTimer(maceManager.getPlugin(), 0L, 2L); // 0.1 second intervals
    }

    private void meteorImpact(Location targetLoc, Player caster) {
        // Massive explosion sound
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.2f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.5f);

        // Damage entities within 6 blocks
        Collection<Entity> nearby = targetLoc.getWorld().getNearbyEntities(targetLoc, 6, 6, 6);

        for (Entity entity : nearby) {
            if (entity != caster && entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;

                // Calculate distance for damage scaling
                double distance = entity.getLocation().distance(targetLoc);
                double damage = Math.max(8, 20 - (distance * 2));

                living.damage(damage);
                entity.setFireTicks(300);

                // Launch entity away
                Vector direction = entity.getLocation().toVector()
                        .subtract(targetLoc.toVector())
                        .normalize()
                        .multiply(4);
                entity.setVelocity(direction);

                // Apply debuffs
                if (living instanceof Player) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 160, 1));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
                    ((Player) living).sendMessage(Component.text("☄️ DEVASTATED BY METEOR SHOWER! ☄️")
                            .color(NamedTextColor.RED));
                }
            }
        }

        // Create crater effect
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location craterLoc = targetLoc.clone().add(x, 0, z);
                int depth = random.nextInt(3) + 2;

                for (int y = 0; y <= depth; y++) {
                    Block block = craterLoc.clone().add(0, -y, 0).getBlock();
                    if (y == depth) {
                        block.setType(Material.LAVA);
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        // Explosion effects
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 150);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 80);
        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 50);
        targetLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, targetLoc, 30);
    }

    private void createWaterGeyser(Player player) {
        Location center = player.getLocation();

        // Create water geyser effect
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) { // 3 seconds
                    cancel();
                    return;
                }

                // Create vertical water effect
                for (int y = 0; y < 10; y++) {
                    Location effectLoc = center.clone().add(0, y, 0);
                    effectLoc.getWorld().spawnParticle(Particle.SPLASH, effectLoc, 20);
                    effectLoc.getWorld().spawnParticle(Particle.BUBBLE, effectLoc, 10);
                }

                // Launch nearby entities upward
                if (ticks % 5 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 3, 3, 3);
                    for (Entity entity : nearby) {
                        if (entity != player) {
                            entity.setVelocity(new Vector(0, 4.0, 0)); // Increased from 1.5 to 4.0 for ~40 block height
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(maceManager.getPlugin(), 0L, 1L);

        player.sendMessage(Component.text("🌊 WATER GEYSER! Launching enemies skyward!")
                .color(NamedTextColor.BLUE));
        center.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 1.0f);
    }

    private void createStoneWall(Player player) {
        Location center = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        // Create a larger 9x5 stone wall in front of player (increased from 5x3)
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));

        for (int x = -4; x <= 4; x++) { // Changed from -2 to 2, now -4 to 4
            for (int y = 0; y < 5; y++) { // Changed from 3 to 5
                Location wallLoc = center.clone()
                        .add(direction.clone().multiply(2))
                        .add(right.clone().multiply(x))
                        .add(0, y, 0);

                Block block = wallLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.STONE);

                    // Schedule wall removal after 30 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            block.setType(Material.AIR);
                        }
                    }.runTaskLater(maceManager.getPlugin(), 600L);
                }
            }
        }

        player.sendMessage(Component.text("🌍 STONE WALL! Created massive protective barrier!")
                .color(NamedTextColor.GREEN));
        center.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.8f);
    }

    private void createEarthquake(Player player) {
        Location center = player.getLocation();

        player.sendMessage(Component.text("🌍 EARTHQUAKE! The earth trembles with your power!")
                .color(NamedTextColor.DARK_GREEN));

        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    cancel();
                    return;
                }

                // Create random stone spikes in 15x15 area (faster - every 5 ticks instead of 10)
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 5; i++) {
                        int randomX = random.nextInt(15) - 7;
                        int randomZ = random.nextInt(15) - 7;

                        Location spikeLoc = center.clone().add(randomX, 0, randomZ);

                        // Don't create spikes directly under the player to prevent suffocation
                        if (spikeLoc.distance(center) < 2) {
                            continue;
                        }

                        int height = random.nextInt(3) + 2;

                        for (int y = 1; y <= height; y++) {
                            Block block = spikeLoc.clone().add(0, y, 0).getBlock();
                            if (block.getType() == Material.AIR) {
                                block.setType(Material.COBBLESTONE);

                                // Schedule spike removal
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        block.setType(Material.AIR);
                                    }
                                }.runTaskLater(maceManager.getPlugin(), 200L);
                            }
                        }
                    }
                }

                // Damage and launch nearby entities
                if (ticks % 20 == 0) {
                    Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 10, 5, 10);
                    for (Entity entity : nearby) {
                        if (entity != player && entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) entity;
                            living.damage(6);

                            Vector knockback = entity.getLocation().toVector()
                                    .subtract(center.toVector())
                                    .normalize()
                                    .multiply(2);
                            knockback.setY(0.5);
                            entity.setVelocity(knockback);

                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                        }
                    }
                }

                // Particle effects
                center.getWorld().spawnParticle(Particle.BLOCK, center, 50, Material.STONE.createBlockData());
                center.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, center, 30);

                ticks++;
            }
        }.runTaskTimer(maceManager.getPlugin(), 0L, 1L);
    }
}