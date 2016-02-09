package me.aqlow.spigot.pvp.util.constraint;

import org.bukkit.inventory.ItemStack;

public interface Constraint {
	public boolean matches(ItemStack item);
}
