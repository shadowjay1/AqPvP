package me.aqlow.spigot.pvp.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MageLoseFocusEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	private Player player;
	
	public MageLoseFocusEvent(Player p) {
		this.player = p;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public static class EventListener implements Listener {
    	@EventHandler
    	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    		Bukkit.getPluginManager().callEvent(new MageLoseFocusEvent(event.getPlayer()));
    	}
    	
    	@EventHandler
    	public void onPlayerQuit(PlayerQuitEvent event) {
    		Bukkit.getPluginManager().callEvent(new MageLoseFocusEvent(event.getPlayer()));
    	}

    	@EventHandler
    	public void onPlayerMove(PlayerMoveEvent event) {
    		if(event.getFrom().getBlock().getLocation().distance(event.getTo().getBlock().getLocation()) > 0) {
    			Bukkit.getPluginManager().callEvent(new MageLoseFocusEvent(event.getPlayer()));
    		}	
    	}

    	@EventHandler
    	public void onPlayerDropItem(PlayerDropItemEvent event) {
    		Bukkit.getPluginManager().callEvent(new MageLoseFocusEvent(event.getPlayer()));
    	}

    	@EventHandler
    	public void onInventoryClick(InventoryClickEvent event) {
    		if(!(event.getWhoClicked() instanceof Player)) return;
    		Bukkit.getPluginManager().callEvent(new MageLoseFocusEvent((Player) event.getWhoClicked()));
    	}
    }
}
