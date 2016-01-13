package me.aqlow.spigot.pvp.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface ItemBlocker {
	public boolean blockItem(Inventory inv, ItemStack item);
}
