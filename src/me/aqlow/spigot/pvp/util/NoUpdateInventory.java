package me.aqlow.spigot.pvp.util;

import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_8_R3.PlayerInventory;

public class NoUpdateInventory extends CraftInventoryPlayer {
	public NoUpdateInventory(PlayerInventory inventory) {
		super(inventory);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void setItem(int index, ItemStack item) {
		getInventory().setItem(index, ((item == null || item.getTypeId() == 0) ? null : CraftItemStack.asNMSCopy(item)));
	}
	
	public static NoUpdateInventory fromInventory(org.bukkit.inventory.PlayerInventory inv) {
		return new NoUpdateInventory(((CraftInventoryPlayer) inv).getInventory());
	}
}
