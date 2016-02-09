package me.aqlow.spigot.pvp.util.constraint;

import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;

public class AnyConstraint extends ArrayList<Constraint> implements Constraint {
	private static final long serialVersionUID = -3816643694408402564L;

	@Override
	public boolean matches(ItemStack item) {
		for(Constraint constraint : this) {
			if(constraint.matches(item)) {
				return true;
			}
		}
		
		return false;
	}
}
