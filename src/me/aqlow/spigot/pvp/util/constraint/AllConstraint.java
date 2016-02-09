package me.aqlow.spigot.pvp.util.constraint;

import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;

public class AllConstraint extends ArrayList<Constraint> implements Constraint {
	private static final long serialVersionUID = 4867581470481499308L;

	@Override
	public boolean matches(ItemStack item) {
		for(Constraint constraint : this) {
			if(!constraint.matches(item)) {
				return false;
			}
		}
		
		return true;
	}
}
