package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {
    private static final String DRAGON_TAG = "skyfang_dragon";
    private static final String PLASMA_TAG = "skyfang_plasma";
    private final Map<UUID, Dragon> dragons = new HashMap<>();
    private NamespacedKey dragonKey;
    private BukkitTask flightTask;

    @Override
    public void onEnable() {
        dragonKey = new NamespacedKey(this, "dragon");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("dragon").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) { sender.sendMessage("Only players can use this command."); return true; }
            String action = args.length == 0 ? "summon" : args[0].toLowerCase();
            Dragon dragon = dragons.get(player.getUniqueId());
            if (action.equals("summon")) {
                if (dragon != null && !dragon.anchor.isDead()) { player.sendMessage(Component.text("Your Skyfang is already summoned.", NamedTextColor.AQUA)); return true; }
                summonDragon(player);
            } else if (dragon == null || dragon.anchor.isDead()) player.sendMessage(Component.text("Summon your Skyfang first with /dragon summon.", NamedTextColor.RED));
            else if (action.equals("attack")) { dragon.attackMode = true; player.sendMessage(Component.text("Skyfang is hunting nearby hostile mobs.", NamedTextColor.BLUE)); }
            else if (action.equals("stay")) { dragon.follow = false; player.sendMessage(Component.text("Skyfang is holding its position.", NamedTextColor.GRAY)); }
            else if (action.equals("follow")) { dragon.follow = true; player.sendMessage(Component.text("Skyfang is following you.", NamedTextColor.AQUA)); }
            else player.sendMessage(Component.text("Use /dragon summon, attack, stay, or follow.", NamedTextColor.YELLOW));
            return true;
        });
        flightTask = Bukkit.getScheduler().runTaskTimer(this, this::tickDragons, 1L, 2L);
    }

    @Override public void onDisable() { if (flightTask != null) flightTask.cancel(); dragons.values().forEach(Dragon::remove); dragons.clear(); }

    private void summonDragon(Player owner) {
        Location spawn = owner.getLocation().add(owner.getLocation().getDirection().multiply(3)).add(0, 3, 0);
        Wolf anchor = (Wolf) owner.getWorld().spawnEntity(spawn, EntityType.WOLF);
        anchor.setCollarColor(DyeColor.BLUE); anchor.setInvisible(true); anchor.setInvulnerable(true); anchor.setSilent(true);
        anchor.addScoreboardTag(DRAGON_TAG); anchor.getPersistentDataContainer().set(dragonKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        Dragon dragon = new Dragon(owner, anchor); dragons.put(owner.getUniqueId(), dragon); dragon.createModel();
        owner.sendMessage(Component.text("Skyfang awaits your bond. Feed it a bone to tame it, then use /dragon attack.", NamedTextColor.AQUA));
        owner.getWorld().playSound(spawn, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.5f);
    }

    private void tickDragons() {
        for (Dragon dragon : new ArrayList<>(dragons.values())) {
            if (dragon.anchor.isDead() || !dragon.owner.isOnline()) { dragon.remove(); dragons.remove(dragon.owner.getUniqueId()); }
            else dragon.tick();
        }
    }

    @EventHandler public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Wolf wolf) || !wolf.getScoreboardTags().contains(DRAGON_TAG)) return;
        event.setCancelled(true); Dragon dragon = dragons.get(event.getPlayer().getUniqueId());
        if (dragon == null || dragon.anchor != wolf) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BONE) {
            event.getPlayer().getInventory().getItemInMainHand().subtract(); wolf.setOwner(event.getPlayer()); wolf.setTamed(true);
            dragon.owner.sendMessage(Component.text("Skyfang accepts your bone and is now tamed.", NamedTextColor.BLUE));
            wolf.getWorld().spawnParticle(Particle.HEART, wolf.getLocation().add(0, 2, 0), 6, .5, .5, .5, 0);
            wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1, 1.3f);
        }
    }

    @EventHandler public void onDamage(EntityDamageByEntityEvent event) { if (event.getEntity().getScoreboardTags().contains(DRAGON_TAG)) event.setCancelled(true); }
    @EventHandler public void onDeath(EntityDeathEvent event) { if (event.getEntity().getScoreboardTags().contains(DRAGON_TAG)) { Dragon dragon = dragons.remove(event.getEntity().getUniqueId()); if (dragon != null) dragon.removeModel(); } }
    @EventHandler public void onQuit(PlayerQuitEvent event) { Dragon dragon = dragons.remove(event.getPlayer().getUniqueId()); if (dragon != null) dragon.remove(); }

    @EventHandler public void onPlasmaHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity(); if (!projectile.getScoreboardTags().contains(PLASMA_TAG)) return;
        if (event.getHitEntity() instanceof LivingEntity target) target.damage(10.0, projectile.getShooter() instanceof Entity source ? source : null);
        Location hit = projectile.getLocation(); hit.getWorld().spawnParticle(Particle.END_ROD, hit, 30, .25, .25, .25, .08); hit.getWorld().playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, .5f, 1.8f);
    }

    private final class Dragon {
        private final Player owner; private final Wolf anchor; private final List<Entity> modelParts = new ArrayList<>();
        private boolean follow = true; private boolean attackMode; private long lastShot;
        private Dragon(Player owner, Wolf anchor) { this.owner = owner; this.anchor = anchor; }
        private void createModel() {
            addPart(Material.BLUE_CONCRETE, 0, 1, 0); addPart(Material.LIGHT_BLUE_CONCRETE, 0, 1.9, .4); addPart(Material.BLUE_CONCRETE, 0, 2.8, .9); addPart(Material.LIGHT_BLUE_CONCRETE, 0, 1.7, -.9);
            addPart(Material.BLUE_CONCRETE, 1.3, .2, .2); addPart(Material.BLUE_CONCRETE, -1.3, .2, .2); addPart(Material.BLUE_CONCRETE, 1.3, .2, -1); addPart(Material.BLUE_CONCRETE, -1.3, .2, -1);
            addPart(Material.LIGHT_BLUE_CONCRETE, 1.8, 1.3, .2); addPart(Material.LIGHT_BLUE_CONCRETE, -1.8, 1.3, .2); addPart(Material.LIGHT_BLUE_CONCRETE, 1.8, 1.3, -.8); addPart(Material.LIGHT_BLUE_CONCRETE, -1.8, 1.3, -.8);
            addPart(Material.BLUE_CONCRETE, .6, 1.5, 1.1); addPart(Material.BLUE_CONCRETE, -.6, 1.5, 1.1);
        }
        private void addPart(Material material, double x, double y, double z) { Entity part = anchor.getWorld().spawnFallingBlock(anchor.getLocation(), material.createBlockData()); part.setGravity(false); part.setInvulnerable(true); part.addScoreboardTag(DRAGON_TAG); modelParts.add(part); }
        private void tick() {
            if (follow) { Location target = owner.getLocation().add(owner.getLocation().getDirection().multiply(3)).add(0, 3, 0); Vector delta = target.toVector().subtract(anchor.getLocation().toVector()); if (delta.lengthSquared() > .2) anchor.setVelocity(delta.normalize().multiply(Math.min(.65, delta.length() * .12))); }
            else anchor.setVelocity(new Vector(0, 0, 0)); updateModel(); if (attackMode && System.currentTimeMillis() - lastShot > 1800) shootNearestHostile();
            anchor.getWorld().spawnParticle(Particle.END_ROD, anchor.getLocation().add(0, 1.2, 0), 2, .35, .35, .35, .01);
        }
        private void updateModel() {
            Location base = anchor.getLocation(); double yaw = Math.toRadians(-base.getYaw()); double[][] o = {{0,1,0},{0,1.9,.4},{0,2.8,.9},{0,1.7,-.9},{1.3,.2,.2},{-1.3,.2,.2},{1.3,.2,-1},{-1.3,.2,-1},{1.8,1.3,.2},{-1.8,1.3,.2},{1.8,1.3,-.8},{-1.8,1.3,-.8},{.6,1.5,1.1},{-.6,1.5,1.1}};
            for (int i = 0; i < modelParts.size(); i++) { double x=o[i][0], z=o[i][2], rx=x*Math.cos(yaw)-z*Math.sin(yaw), rz=x*Math.sin(yaw)+z*Math.cos(yaw); modelParts.get(i).teleport(base.clone().add(rx,o[i][1],rz)); }
        }
        private void shootNearestHostile() {
            LivingEntity target = null; double best = 324;
            for (Entity entity : anchor.getNearbyEntities(18, 10, 18)) if (entity instanceof Monster monster && monster != anchor && entity.getScoreboardTags().stream().noneMatch(DRAGON_TAG::equals) && entity.getLocation().distanceSquared(anchor.getLocation()) < best) { best=entity.getLocation().distanceSquared(anchor.getLocation()); target=monster; }
            if (target == null) return; Location muzzle=anchor.getLocation().add(anchor.getLocation().getDirection().multiply(1.8)).add(0,1.3,0); Snowball blast=anchor.getWorld().spawn(muzzle,Snowball.class); blast.setShooter(owner); blast.addScoreboardTag(PLASMA_TAG); blast.setItem(new org.bukkit.inventory.ItemStack(Material.BLUE_DYE)); blast.setVelocity(target.getEyeLocation().toVector().subtract(muzzle.toVector()).normalize().multiply(1.4)); anchor.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,muzzle,18,.2,.2,.2,.1); anchor.getWorld().playSound(muzzle,Sound.ENTITY_GUARDIAN_ATTACK,.8f,1.5f); lastShot=System.currentTimeMillis();
        }
        private void removeModel() { modelParts.forEach(Entity::remove); modelParts.clear(); }
        private void remove() { removeModel(); anchor.remove(); }
    }
}
