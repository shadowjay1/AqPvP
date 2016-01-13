package me.aqlow.spigot.pvp.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.aqlow.spigot.pvp.AqPvP;

public class ItemBlockerManager implements Listener {
	public static ItemBlockerManager instance = new ItemBlockerManager();
	private ArrayList<ItemBlocker> blockers = new ArrayList<ItemBlocker>();
	
	public void addBlocker(ItemBlocker blocker) {
		blockers.add(blocker);
	}
	
	@EventHandler
	public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		if(blockItem(event.getInventory(), event.getItem().getItemStack())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryMoveItem(InventoryMoveItemEvent event) {
		if(blockItem(event.getDestination(), event.getItem())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryPickupItem(PlayerPickupItemEvent event) {
		if(blockItem(event.getPlayer().getInventory(), event.getItem().getItemStack())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if(event.isCancelled() || event.getOldCursor() == null) return;
		
		event.setCancelled(true);
		final ItemStack cursor = event.getOldCursor();
		final InventoryView view = event.getView();
		final HashMap<Integer, ItemStack> changes = new HashMap<Integer, ItemStack>();
		
		for(int slot : event.getNewItems().keySet()) {
			ItemStack item = event.getNewItems().get(slot);
			if(item == null) continue;
			if(slot < view.getTopInventory().getSize()) {
				if(!blockItem(view.getTopInventory(), item)) {
					changes.put(slot, item);
				}
			}
			else {
				if(!blockItem(view.getBottomInventory(), item)) {
					changes.put(slot, item);
				}
			}
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(AqPvP.instance, new Runnable() {
			public void run() {
				for(Integer slot : changes.keySet()) {
					int amount = cursor.getAmount() - changes.get(slot).getAmount() + view.getItem(slot).getAmount();
					view.setItem(slot, changes.get(slot));
					cursor.setAmount(amount);
				}
				view.setCursor(cursor);
			}
		});
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			InventoryView view = event.getView();
			Inventory other;
			if(view.getTopInventory() != null && !view.getTopInventory().equals(event.getClickedInventory())) {
				other = view.getTopInventory();
			}
			else {
				other = view.getBottomInventory();
			}
			
			if(blockItem(other, event.getCurrentItem())) {
				event.setCancelled(true);
			}
		}
		else if(event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_SOME || event.getAction() == InventoryAction.PLACE_ONE) {
			if(blockItem(event.getClickedInventory(), event.getCursor())) {
				event.setCancelled(true);
			}
		}
		else if(event.getAction() == InventoryAction.HOTBAR_SWAP) {
			if(blockItem(event.getView().getBottomInventory(), event.getCurrentItem())) {
				event.setCancelled(true);
			}
		}
		else if(event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
			if(blockItem(event.getClickedInventory(), event.getCursor())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
		ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
		
		if(newItem == null) return;
		
		NoUpdateInventory inventory = NoUpdateInventory.fromInventory(event.getPlayer().getInventory());
		inventory.setItem(event.getNewSlot(), null);
		
		if(blockItem(inventory, newItem)) {
			event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), newItem);
		}
		else {
			event.getPlayer().getInventory().setItem(event.getNewSlot(), newItem);
		}
	}
	
	private boolean blockItem(Inventory inv, ItemStack item) {
		for(ItemBlocker blocker : blockers) {
			if(blocker.blockItem(inv, item)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void checkPlayerInventory(Player player, ItemBlocker blocker) {
		PlayerInventory inv = player.getInventory();
		
		for(int i = 0; i < inv.getSize(); i++) {
			if(blocker.blockItem(inv, inv.getItem(i))) {
				player.getWorld().dropItem(player.getLocation(), inv.getItem(i));
				inv.setItem(i, null);
			}
		}
	}
}
