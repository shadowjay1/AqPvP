package me.aqlow.spigot.pvp.util.constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LoreConstraint implements Constraint {
	private final List<String> lore;
	
	public LoreConstraint() {
		lore = new ArrayList<String>();
	}
	
	public LoreConstraint(String[] lore) {
		this.lore = Arrays.asList(lore);
	}
	
	@Override
	public boolean matches(ItemStack item) {
		if(item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if(meta.hasLore()) {
				List<String> itemLore = meta.getLore();
				
				if(itemLore.size() != lore.size()) return false;
				
				for(int i = 0; i < itemLore.size(); i++) {
					if(!itemLore.get(i).equals(lore.get(i))) {
						return false;
					}
				}
				
				return true;
			}
		}
		
		return lore.size() == 0;
	}
}
