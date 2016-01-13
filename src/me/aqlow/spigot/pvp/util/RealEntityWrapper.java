package me.aqlow.spigot.pvp.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class RealEntityWrapper implements EntityWrapper {
	private Entity entity;
	
	public RealEntityWrapper(Entity entity) {
		this.entity = entity;
	}
	
	@Override
	public Location getLocation() {
		return entity.getLocation();
	}
	
	@Override
	public boolean isDead() {
		return entity.isDead();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Entity) {
			return ((Entity) o).equals(entity);
		}
		else if(o instanceof RealEntityWrapper) {
			return ((RealEntityWrapper) o).entity.equals(entity);
		}
		else {
			return false;
		}
	}
	
	@Override
	public void remove() {
		entity.remove();
	}
}
