package me.aqlow.spigot.pvp.spells;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.util.AABB;
import me.aqlow.spigot.pvp.util.AqUtil;
import me.aqlow.spigot.pvp.util.EntityWrapper;
import me.aqlow.spigot.pvp.util.PseudoEntityWrapper;
import me.aqlow.spigot.pvp.util.Ray;

public class SpellProjectile {
	private static final double ADDED_ENTITY_RADIUS = 5;
	
	private Player shooter = null;
	private EntityWrapper entity;
	private Location lastLocation = null;
	private double customRadius = -1;
	private SpellProjectileHandler handler = new SpellProjectileHandler() {
		@Override
		public boolean onHit(SpellProjectile proj, Entity e) { return false; }
		@Override
		public void onTick(SpellProjectile proj) {}
	};
	
	public SpellProjectile(EntityWrapper e) {
		this.entity = e;
	}
	
	public SpellProjectile(EntityWrapper e, double customRadius) {
		this.entity = e;
		this.customRadius = customRadius;
	}
	
	public void setHandler(SpellProjectileHandler handler) {
		this.handler = handler;
	}
	
	public Player getShooter() {
		return shooter;
	}
	
	public void setShooter(Player p) {
		this.shooter = p;
	}
	
	public EntityWrapper getEntity() {
		if(entity.isDead()) return null;
		return entity;
	}
	
	public Location getLastLocation() {
		if(lastLocation == null) return null;
		return lastLocation.clone();
	}
	
	public void activate() {
		activeProjectiles.add(this);
	}
	
	private static ArrayList<SpellProjectile> activeProjectiles = new ArrayList<SpellProjectile>();
	private static int previousTask = -1;
	public static void load(FileConfiguration config) {
		if(previousTask >= 0) {
			Bukkit.getServer().getScheduler().cancelTask(previousTask);
		}
		previousTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				Iterator<SpellProjectile> iterator = activeProjectiles.iterator();
				
				while(iterator.hasNext()) {
					SpellProjectile proj = iterator.next();
					EntityWrapper e = proj.getEntity();
					if(e == null) {
						iterator.remove();
						continue;
					}
					
					if(proj.lastLocation != null && proj.lastLocation.getWorld().equals(e.getLocation().getWorld())) {
						Ray ray = new Ray(proj.lastLocation.toVector(), e.getLocation().toVector().subtract(proj.lastLocation.toVector()));
						
						List<Entity> potentialTargets = AqUtil.getEntitiesInRadius(proj.lastLocation, proj.customRadius + ray.getLength() + ADDED_ENTITY_RADIUS);
						for(Entity potentialTarget : potentialTargets) {
							AABB boundingBox = new AABB(((CraftEntity) potentialTarget).getHandle().getBoundingBox());
							boundingBox = boundingBox.expand(proj.customRadius);
							double result = ray.collide(boundingBox);
							if(result < ray.getLength() && result >= 0) {
								if(proj.handler.onHit(proj, potentialTarget)) {
									iterator.remove();
									break;
								}
							}
						}
					}
					proj.handler.onTick(proj);
					proj.lastLocation = e.getLocation().clone();
					if(e instanceof PseudoEntityWrapper) {
						((PseudoEntityWrapper) e).onTick();
					}
				}
			}
		}, 0, 1);
	}
	
	public static void registerListener() {
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onEntityDamage(EntityDamageByEntityEvent event) {
				if(event.getDamager() instanceof Projectile) {
					Iterator<SpellProjectile> iterator = activeProjectiles.iterator();
					
					while(iterator.hasNext()) {
						SpellProjectile proj = iterator.next();
						EntityWrapper e = proj.getEntity();
						if(e == null) {
							iterator.remove();
							continue;
						}
						
						if(e.equals(event.getDamager())) {
							event.setCancelled(true);
							if(proj.handler.onHit(proj, event.getEntity())) {
								iterator.remove();
							}
							break;
						}
					}
				}
			}
			
			@EventHandler
			public void onEntityExplode(EntityExplodeEvent event) {
				if(event.getEntity() instanceof Projectile) {
					Iterator<SpellProjectile> iterator = activeProjectiles.iterator();
					
					while(iterator.hasNext()) {
						SpellProjectile proj = iterator.next();
						EntityWrapper e = proj.getEntity();
						if(e == null) {
							iterator.remove();
							continue;
						}
						
						if(e.equals(event.getEntity())) {
							event.setCancelled(true);
							iterator.remove();
							break;
						}
					}
				}
			}
		}, AqPvP.instance);
	}
	
	public interface SpellProjectileHandler {
		// if false is returned the projectile does not hit the target
		// should dispose of projectile entity
		public boolean onHit(SpellProjectile proj, Entity e);
		public void onTick(SpellProjectile proj);
	}
}
