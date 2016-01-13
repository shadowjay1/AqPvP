package me.aqlow.spigot.pvp.spells;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SpellUtil {
	public static boolean removeItems(PlayerInventory inv, ItemStack[] items) {
		ItemStack[] original = inv.getContents();
		
		HashMap<Integer, ItemStack> leftOver = inv.removeItem(items);
		// TODO - check armor slots
		
		if(leftOver.isEmpty()) {
			return true;
		}
		else {
			inv.setContents(original);
			return false;
		}
	}
}
