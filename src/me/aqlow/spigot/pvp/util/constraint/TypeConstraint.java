package me.aqlow.spigot.pvp.util.constraint;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TypeConstraint implements Constraint {
	private final Material type;
	
	public TypeConstraint(Material type) {
		this.type = type;
	}

	@Override
	public boolean matches(ItemStack item) {
		if(item == null) return type == Material.AIR;
		
		return item.getType() == type;
	}
}
