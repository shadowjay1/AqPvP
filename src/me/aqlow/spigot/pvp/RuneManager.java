package me.aqlow.spigot.pvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.aqlow.spigot.pvp.runes.Rune;
import me.aqlow.spigot.pvp.util.ItemBlocker;
import me.aqlow.spigot.pvp.util.ItemBlockerManager;
import net.md_5.bungee.api.ChatColor;

public class RuneManager implements Listener {
	public static final RuneManager instance = new RuneManager();
	
	public static float MAGIC_DAMAGE_REDUCTION = 0.5F;
	public static float LAUNCHER_COOLDOWN_REDUCTION = 0.25F;
	public static float POWER_LAUNCHER_MULTIPLIER = 2F;
	public static int HUNGER_REGEN_TICKS = 20 * 5; // 20 ticks * 5 seconds
	public static float TENACITY_REDUCTION = 0.4F;
	public static long VANISH_INACTION_MS = 5000;
	public static long RAGE_LENGTH_MS = 1000;
	public static float RAGE_SPEED = 0.3F; // 0.2 = default
	
	private static int lastHungerTask = -1;
	private static int lastVanishTask = -1;
	private static int lastRageTask = -1;
	
	private ItemBlocker runeBlocker = new ItemBlocker() {
		@Override
		public boolean blockItem(Inventory inv, ItemStack item) {
			if(!(inv instanceof PlayerInventory)) return false;
			
			if(getRune(item) == null) return false;
			
			for(int i = 0; i < inv.getSize(); i++) {
				if(getRune(inv.getItem(i)) != null) {
					return true;
				}
			}
			
			return false;
		}
	};
	
	private HashMap<String, Long> lastAction = new HashMap<String, Long>();
	private ArrayList<String> stealthedPlayers = new ArrayList<String>();
	
	public RuneManager() {
		ItemBlockerManager.instance.addBlocker(runeBlocker);
	}
	
	public static void load(FileConfiguration config) {
		MAGIC_DAMAGE_REDUCTION = (float) config.getDouble("runes.magic-damage-reduction", 0.5);
		LAUNCHER_COOLDOWN_REDUCTION = (float) config.getDouble("runes.launcher-cooldown-reduction", 0.5);
		HUNGER_REGEN_TICKS = config.getInt("runes.hunger-regen-ticks", 20 * 5);
		TENACITY_REDUCTION = (float) config.getDouble("runes.tenacity-reduction", 0.4);
		VANISH_INACTION_MS = config.getLong("runes.vanish-inaction-ms", 5000);
		RAGE_LENGTH_MS = config.getLong("runes.rage-length-ms", 1000);
		RAGE_SPEED = (float) config.getDouble("runes.rage-speed", 0.3);
		long VANISH_TASK_TICKS = config.getInt("runes.vanish-task-ticks", 10);
		long RAGE_TASK_TICKS = config.getInt("runes.rage-task-ticks", 1);
		
		if(lastHungerTask > -1) Bukkit.getScheduler().cancelTask(lastHungerTask);
		if(lastVanishTask > -1) Bukkit.getScheduler().cancelTask(lastVanishTask);
		if(lastRageTask > -1) Bukkit.getScheduler().cancelTask(lastRageTask);
		
		lastHungerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(instance.hasRune(p, Rune.HUNGER_REGEN)) {
						if(p.getFoodLevel() < 20) {
							p.setFoodLevel(p.getFoodLevel() + 1);
						}
						else if(p.getSaturation() <= 19) {
							p.setSaturation(p.getSaturation() + 1);
						}
					}
				}
			}
		}, 0, HUNGER_REGEN_TICKS);
		
		lastVanishTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(instance.hasRune(p, Rune.AMBUSH)) {
						if(System.currentTimeMillis() - instance.getLastActionTime(p) > VANISH_INACTION_MS) {
							instance.stealthPlayer(p);
						}
						else {
							instance.unstealthPlayer(p);
						}
					}
					else {
						instance.unstealthPlayer(p);
					}
				}
			}
		}, 0, VANISH_TASK_TICKS);
		
		lastRageTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(instance.hasRune(p, Rune.RAGE)) {
						if(AttackManager.instance.getLastAttack(p.getName()) < RAGE_LENGTH_MS) {
							if(p.getWalkSpeed() != RAGE_SPEED) {
								p.setWalkSpeed(RAGE_SPEED);
							}
						}
						else {
							if(p.getWalkSpeed() != 0.2F) {
								p.setWalkSpeed(0.2F);
							}
						}
					}
					else {
						if(p.getWalkSpeed() != 0.2F) {
							p.setWalkSpeed(0.2F);
						}
					}
				}
			}
		}, 0, RAGE_TASK_TICKS);
	}
	
	@EventHandler
	public void stealthAllOnLogin(PlayerJoinEvent event) {
		Iterator<String> iterator = stealthedPlayers.iterator();
		
		while(iterator.hasNext()) {
			String stealthedName = iterator.next();
			Player stealthed = Bukkit.getPlayerExact(stealthedName);
			if(stealthed == null) {
				iterator.remove();
				continue;
			}
			event.getPlayer().hidePlayer(stealthed);
		}
	}
	
	public void stealthPlayer(Player p) {
		if(stealthedPlayers.contains(p.getName())) return;
		
		stealthedPlayers.add(p.getName());
		
		for(Player viewer : Bukkit.getOnlinePlayers()) {
			viewer.hidePlayer(p);
		}
		
		p.sendMessage(ChatColor.GRAY + "You have stealthed.");
	}
	
	public void unstealthPlayer(Player p) {
		if(!stealthedPlayers.remove(p.getName())) return;
		
		for(Player viewer : Bukkit.getOnlinePlayers()) {
			viewer.showPlayer(p);
		}
		
		p.sendMessage(ChatColor.GRAY + "You are no longer stealthed.");
	}
	
	public long getLastActionTime(Player p) {
		if(lastAction.containsKey(p.getName())) {
			return lastAction.get(p.getName());
		}
		else {
			return 0;
		}
	}
	
	public Rune getRune(ItemStack item) {
		if(item == null) return null;
		if(item.getType() != Material.ENCHANTED_BOOK) return null;
		if(!item.hasItemMeta()) return null;
		if(!item.getItemMeta().hasLore()) return null;
		if(item.getItemMeta().getLore().size() < 1) return null;
		return Rune.getById(item.getItemMeta().getLore().get(0));
	}
	
	public boolean hasRune(Player p, Rune rune) {
		for(ItemStack item : p.getInventory().getContents()) {
			if(getRune(item) == rune) {
				return true;
			}
		}
		
		return false;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if(!event.getFrom().toVector().equals(event.getTo().toVector())) {
			lastAction.put(event.getPlayer().getName(), System.currentTimeMillis());
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		lastAction.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		lastAction.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player) {
			lastAction.put(event.getDamager().getName(), System.currentTimeMillis());
		}
	}
}
