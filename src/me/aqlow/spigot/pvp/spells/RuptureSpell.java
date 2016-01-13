package me.aqlow.spigot.pvp.spells;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.util.AqUtil;

public class RuptureSpell implements Castable, Targetable {
	private ArrayList<Rupture> ruptures = new ArrayList<Rupture>();
	
	public static int COOLDOWN = 0;
	public static int COST = 0;
	
	public static double PARTICLES_PER_CUBE = 0.5;
	public static double EXPLOSION_PARTICLE_MULTIPLIER = 20;
	
	public RuptureSpell() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				Iterator<Rupture> iterator = ruptures.iterator();
				
				while(iterator.hasNext()) {
					Rupture rupture = iterator.next();
					boolean collapse = false;
					
					// check age
					if(System.currentTimeMillis() > rupture.collapseTime) {
						collapse = true;
					}
					
					// check projectile limit
					ArrayList<Entity> entities = AqUtil.getEntitiesInRadius(rupture.location, rupture.radius, Entity.class);
					Iterator<Entity> eIterator = entities.iterator();
					while(eIterator.hasNext()) {
						if(eIterator.next() instanceof Player) {
							eIterator.remove();
						}
					}
					if(entities.size() > rupture.projectileLimit) {
						collapse = true;
					}
					
					// if not collapsing, pull all projectiles towards center
					if(!collapse) {
						for(Entity entity : entities) {
							entity.setVelocity(rupture.location.clone().subtract(entity.getLocation()).toVector().normalize().multiply(rupture.gradualForce));
						}
						
						for(int i = 0; i < rupture.particleCount; i++) {
							double randomRadius = Math.random() * rupture.radius;
							double randomTheta = Math.random() * 2 * Math.PI;
							double randomZ = Math.random() * 2 - 1;
							double randomX = Math.sqrt(1 - (randomZ * randomZ)) * Math.cos(randomTheta);
							double randomY = Math.sqrt(1 - (randomZ * randomZ)) * Math.sin(randomTheta);
							randomX *= randomRadius;
							randomY *= randomRadius;
							randomZ *= randomRadius;
							
							Vector v = new Vector(randomX, randomY, randomZ);
							Location location = rupture.location.clone().add(v);
							
							float multiplier = -0.09F;
							float yAddition = 1F / (float) Math.max(randomRadius, 3);
							rupture.location.getWorld().spigot().playEffect(location, Effect.TILE_DUST, Material.BEDROCK.getId(), 0, (float) randomX * multiplier, (float) randomY * multiplier + yAddition, (float) randomZ * multiplier, 1, 0, 50);
						}
					}
					// if collapsing, push all entities away from center
					else {
						for(Entity entity : entities) {
							if(entity.getLocation().distance(rupture.location) < 1.0D) {
								double randomTheta = Math.random() * 2 * Math.PI;
								double randomZ = Math.random() * 2 - 1;
								double randomX = Math.sqrt(1 - (randomZ * randomZ)) * Math.cos(randomTheta);
								double randomY = Math.sqrt(1 - (randomZ * randomZ)) * Math.sin(randomTheta);
								
								entity.setVelocity(new Vector(randomX, randomY, randomZ).multiply(rupture.explosionForce));
							}
							else {
								entity.setVelocity(rupture.location.clone().subtract(entity.getLocation()).toVector().normalize().multiply(rupture.explosionForce));
							}
						}
						
						for(int i = 0; i < rupture.particleCount * EXPLOSION_PARTICLE_MULTIPLIER; i++) {
							double randomRadius = Math.random() * rupture.radius;
							double randomTheta = Math.random() * 2 * Math.PI;
							double randomZ = Math.random() * 2 - 1;
							double randomX = Math.sqrt(1 - (randomZ * randomZ)) * Math.cos(randomTheta);
							double randomY = Math.sqrt(1 - (randomZ * randomZ)) * Math.sin(randomTheta);
							randomX *= randomRadius;
							randomY *= randomRadius;
							randomZ *= randomRadius;
							
							Vector v = new Vector(randomX, randomY, randomZ);
							Location location = rupture.location.clone().add(v);
							
							rupture.location.getWorld().spigot().playEffect(location, Effect.TILE_DUST, Material.BEDROCK.getId(), 0, (float) randomX * 0.2F, (float) randomY * 0.2F, (float) randomZ * 0.2F, 1, 0, 50);
						}
						
						iterator.remove();
					}
				}
			}
		}, 1, 1);
	}
	
	@Override
	public String getName() {
		return "Rupture";
	}

	@Override
	public int getCost() {
		return COST;
	}

	@Override
	public long getCooldown() {
		return COOLDOWN;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		return cast(p);
	}

	@Override
	public boolean cast(Player p) {
		ruptures.add(new Rupture(p.getLocation().add(0, 4, 0), 500, 20000, 10, 0.2F, 1.0F));
		
		return true;
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN = config.getInt("mage.rupture-spell.cooldown", 0);
		COST = config.getInt("mage.rupture-spell.cost", 0);
	}
	
	private class Rupture {
		private final Location location;
		private int projectileLimit;
		private long collapseTime;
		private double radius;
		private float gradualForce;
		private float explosionForce;
		private int particleCount;
		
		private Rupture(Location location, int projectileLimit, long lifespan, float radius, float gradualForce, float explosionForce) {
			this.location = location;
			this.projectileLimit = projectileLimit;
			this.collapseTime = System.currentTimeMillis() + lifespan;
			this.radius = radius;
			this.gradualForce = gradualForce;
			this.explosionForce = explosionForce;
			this.particleCount = 50;//(int) Math.round(PARTICLES_PER_CUBE * (4F/3F) * Math.PI * Math.pow(radius, 3));
		}
	}
}
