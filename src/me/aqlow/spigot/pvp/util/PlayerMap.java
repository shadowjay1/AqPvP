package me.aqlow.spigot.pvp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.aqlow.spigot.pvp.AqPvP;

public class PlayerMap<T> implements Listener {
	private HashMap<UUID, T> map = new HashMap<UUID, T>();
	private final boolean onlineOnly;
	
	public PlayerMap() {
		Bukkit.getPluginManager().registerEvents(this, AqPvP.instance);
		this.onlineOnly = true;
	}
	
	public PlayerMap(boolean onlineOnly) {
		Bukkit.getPluginManager().registerEvents(this, AqPvP.instance);
		this.onlineOnly = onlineOnly;
	}
	
	public Set<Player> keySet() {
		HashSet<Player> players = new HashSet<Player>();
		
		for(UUID uuid : map.keySet()) {
			Player p = Bukkit.getPlayer(uuid);
			
			if(p != null) {
				players.add(p);
			}
		}
		
		return players;
	}
	
	public Set<OfflinePlayer> keySetOffline() {
		HashSet<OfflinePlayer> players = new HashSet<OfflinePlayer>();
		
		for(UUID uuid : map.keySet()) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
			
			if(p != null) {
				players.add(p);
			}
		}
		
		return players;
	}
	
	public void put(Player p, T obj) {
		map.put(p.getUniqueId(), obj);
	}
	
	public T get(Player p) {
		return map.get(p.getUniqueId());
	}
	
	public boolean contains(Player p) {
		return map.containsKey(p.getUniqueId());
	}
	
	public T remove(Player p) {
		return map.remove(p.getUniqueId());
	}
	
	public void removeAll(Collection<Player> players) {
		for(Player p : players) {
			this.remove(p);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if(onlineOnly) {
			map.remove(event.getPlayer().getUniqueId());
		}
	}
}
