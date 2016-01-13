package me.aqlow.spigot.pvp.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class PseudoEntityWrapper implements EntityWrapper {
	private Location initialLoc;
	private Location loc;
	private Vector vel;
	private double range;
	
	public PseudoEntityWrapper(Location loc, Vector vel, double range) {
		this.initialLoc = loc.clone();
		this.loc = loc.clone();
		this.vel = vel;
		this.range = range;
	}
	
	public void onTick() {
		this.loc.add(vel);
	}
	
	@Override
	public Location getLocation() {
		return loc;
	}
	
	@Override
	public boolean isDead() {
		return this.loc.distance(initialLoc) > range;
	}
	
	@Override
	public void remove() {}
}
