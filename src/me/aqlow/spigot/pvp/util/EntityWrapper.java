package me.aqlow.spigot.pvp.util;

import org.bukkit.Location;

public interface EntityWrapper {
	public Location getLocation();
	public boolean isDead();
	public void remove();
}
