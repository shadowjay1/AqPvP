package me.aqlow.spigot.pvp;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import me.aqlow.spigot.pvp.damage.AqEntityDamageByEntityEvent;
import me.aqlow.spigot.pvp.spells.Castable;
import me.aqlow.spigot.pvp.spells.DebugSpell;
import me.aqlow.spigot.pvp.spells.DrainSpell;
import me.aqlow.spigot.pvp.spells.LaunchSpell;
import me.aqlow.spigot.pvp.spells.PullSpell;
import me.aqlow.spigot.pvp.spells.RuptureSpell;
import me.aqlow.spigot.pvp.spells.SmiteSpell;
import me.aqlow.spigot.pvp.spells.SnareSpell;
import me.aqlow.spigot.pvp.spells.Spell;
import me.aqlow.spigot.pvp.spells.SummonSpell;
import me.aqlow.spigot.pvp.spells.Targetable;
import me.aqlow.spigot.pvp.util.ItemBlocker;
import me.aqlow.spigot.pvp.util.ItemBlockerManager;
import net.md_5.bungee.api.ChatColor;

public class MagicManager implements Listener {
	public static final MagicManager instance = new MagicManager();
	
	private static Material WAND_MATERIAL = Material.BLAZE_ROD;
	// Array of active spells
	private Spell[] spells = new Spell[] {new LaunchSpell(), new SmiteSpell(), new PullSpell(), new DrainSpell(), new SnareSpell(), new RuptureSpell(), new DebugSpell(),
			new SummonSpell(Bat.class, 10, 60000), new SummonSpell(Blaze.class, 10, 240000 * 0)};
	// Ab (Armor bonus) values per armor piece
	private static HashMap<Material, Float> armor = new HashMap<Material, Float>();
	
	// Player spell cooldowns
	private HashMap<String, HashMap<Spell, Long>> cooldowns = new HashMap<String, HashMap<Spell, Long>>();
	
	// components necessary to make armor piece / components necessary to make full armor set
	public static float HELMET_FACTOR = 5F / 24F;
	public static float CHESTPLATE_FACTOR = 8F / 24F;
	public static float LEGGINGS_FACTOR = 7F / 24F;
	public static float BOOTS_FACTOR = 4F / 24F;
	
	// arbitrary armor bonus values
	public static float DIAMOND_BONUS = 0F;
	public static float IRON_BONUS = 1F;
	public static float GOLD_BONUS = 2F;
	public static float CLOTH_BONUS = 1F;
	public static float CHAINMAIL_BONUS = 3F;
	
	private ItemBlocker foodBlocker = new ItemBlocker() {
		@Override
		public boolean blockItem(Inventory inv, ItemStack item) {
			if(!(inv instanceof PlayerInventory)) {
				return false;
			}
			
			if(hasSpell(inv) && item.getType().isEdible()) {
				return true;
			}
			else if(getSpell(item) != null && hasFood(inv)) {
				return true;
			}
			
			return false;
		}
	};
	
	public MagicManager() {
		ItemBlockerManager.instance.addBlocker(foodBlocker);
		
		for(Spell spell : spells) {
			System.out.println("Spell: " + spell.getName());
			if(spell instanceof Listener) {
				Bukkit.getPluginManager().registerEvents((Listener) spell, AqPvP.instance);
				System.out.println("Registered spell: " + spell.getName());
			}
		}
	}
	
	public static void load(FileConfiguration config) {
		DIAMOND_BONUS = (float) config.getDouble("mage.armor-bonus.diamond", 0);
		IRON_BONUS = (float) config.getDouble("mage.armor-bonus.iron", 1);
		GOLD_BONUS = (float) config.getDouble("mage.armor-bonus.gold", 2);
		CLOTH_BONUS = (float) config.getDouble("mage.armor-bonus.cloth", 1);
		CHAINMAIL_BONUS = (float) config.getDouble("mage.armor-bonus.chainmail", 3);
		
		armor.put(Material.DIAMOND_HELMET, DIAMOND_BONUS * HELMET_FACTOR);
		armor.put(Material.DIAMOND_CHESTPLATE, DIAMOND_BONUS * CHESTPLATE_FACTOR);
		armor.put(Material.DIAMOND_LEGGINGS, DIAMOND_BONUS * LEGGINGS_FACTOR);
		armor.put(Material.DIAMOND_BOOTS, DIAMOND_BONUS * BOOTS_FACTOR);
		armor.put(Material.IRON_HELMET, IRON_BONUS * HELMET_FACTOR);
		armor.put(Material.IRON_CHESTPLATE, IRON_BONUS * CHESTPLATE_FACTOR);
		armor.put(Material.IRON_LEGGINGS, IRON_BONUS * LEGGINGS_FACTOR);
		armor.put(Material.IRON_BOOTS, IRON_BONUS * BOOTS_FACTOR);
		armor.put(Material.GOLD_HELMET, GOLD_BONUS * HELMET_FACTOR);
		armor.put(Material.GOLD_CHESTPLATE, GOLD_BONUS * CHESTPLATE_FACTOR);
		armor.put(Material.GOLD_LEGGINGS, GOLD_BONUS * LEGGINGS_FACTOR);
		armor.put(Material.GOLD_BOOTS, GOLD_BONUS * BOOTS_FACTOR);
		armor.put(Material.LEATHER_HELMET, CLOTH_BONUS * HELMET_FACTOR);
		armor.put(Material.LEATHER_CHESTPLATE, CLOTH_BONUS * CHESTPLATE_FACTOR);
		armor.put(Material.LEATHER_LEGGINGS, CLOTH_BONUS * LEGGINGS_FACTOR);
		armor.put(Material.LEATHER_BOOTS, CLOTH_BONUS * BOOTS_FACTOR);
		armor.put(Material.CHAINMAIL_HELMET, CHAINMAIL_BONUS * HELMET_FACTOR);
		armor.put(Material.CHAINMAIL_CHESTPLATE, CHAINMAIL_BONUS * CHESTPLATE_FACTOR);
		armor.put(Material.CHAINMAIL_LEGGINGS, CHAINMAIL_BONUS * LEGGINGS_FACTOR);
		armor.put(Material.CHAINMAIL_BOOTS, CHAINMAIL_BONUS * BOOTS_FACTOR);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.hasItem()) {			
			// cast Castable spells
			Player player = event.getPlayer();
			Spell spell = getSpell(event.getItem());
			if(spell != null) {
				if(isOffCooldown(player.getName(), spell)) {
					if(spell instanceof Castable) {
						if(((Castable) spell).cast(player)) {
							reducePlayerResource(player, spell.getCost());
							putOnCooldown(player.getName(), spell);
						}
						event.setCancelled(true);
					}
				}
				else {
					notifyCooldown(player, spell);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		
		// cast Targetable spells
		if(player.getItemInHand() != null) {
			Spell spell = getSpell(player.getItemInHand());
			
			if(spell != null && spell instanceof Targetable) {
				if(isOffCooldown(player.getName(), spell)) {
					if(((Targetable) spell).cast(player, event.getRightClicked())) {
						reducePlayerResource(player, spell.getCost());
						putOnCooldown(player.getName(), spell);
					}
					event.setCancelled(true);
				}
				else {
					notifyCooldown(player, spell);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
		if(event.getCause() != DamageCause.ENTITY_ATTACK || event instanceof AqEntityDamageByEntityEvent) return;
		
		// cast Targetable spells
		if(event.getDamager() instanceof Player) {
			Player player = (Player) event.getDamager();
			
			if(player.getItemInHand() != null) {
				Spell spell = getSpell(player.getItemInHand());
				
				if(spell != null && spell instanceof Targetable) {
					if(isOffCooldown(player.getName(), spell)) {
						if(((Targetable) spell).cast(player, event.getEntity())) {
							reducePlayerResource(player, spell.getCost());
							putOnCooldown(player.getName(), spell);
						}
						event.setCancelled(true);
					}
					else {
						notifyCooldown(player, spell);
					}
				}
			}
		}
	}
	
	public int getPlayerResource(Player player) {
		return (int) (player.getFoodLevel() + Math.floor(player.getSaturation()));
	}
	
	public void reducePlayerResource(Player player, int cost) {
		if(player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		
		// this isn't exactly the most efficient way to do this, but I'm lazy
		for(; cost > 0; cost--) {
			if(player.getSaturation() >= 1) {
				player.setSaturation(player.getSaturation() - 1);
			}
			else if(player.getFoodLevel() >= 1) {
				player.setFoodLevel(player.getFoodLevel() - 1);
			}
			else if(player.getHealth() >= 1) {
				player.setHealth(player.getHealth() - 1);
			}
			else {
				player.setHealth(0);
			}
		}
	}
	
	public Spell getSpell(ItemStack wand) {
		if(wand.getType() == WAND_MATERIAL && wand.hasItemMeta()) {
			ItemMeta meta = wand.getItemMeta();
			if(meta.getLore() != null && meta.getLore().size() >= 1) {
				String spellName = meta.getLore().get(0);
				
				for(Spell spell : spells) {
					if(spell.getName().equals(spellName)) {
						return spell;
					}
				}
			}
		}
		
		return null;
	}
	
	public boolean hasSpell(Player p) {
		ItemStack[] items = p.getInventory().getContents();
		
		for(ItemStack item : items) {
			if(item != null && getSpell(item) != null) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasSpell(Inventory inv) {
		ItemStack[] items = inv.getContents();
		
		for(ItemStack item : items) {
			if(item != null && getSpell(item) != null) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasFood(Inventory inv) {
		ItemStack[] items = inv.getContents();
		
		for(ItemStack item : items) {
			if(item != null && item.getType().isEdible()) {
				return true;
			}
		}
		
		return false;
	}
	
	public void removeAllFood(Player p) {
		ItemStack[] items = p.getInventory().getContents();
		
		for(int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			
			if(item != null && item.getType().isEdible()) {
				p.getWorld().dropItem(p.getLocation(), item);
				p.getInventory().setItem(i, null);
			}
		}
	}
	
	public void putOnCooldown(String player, Spell spell) {
		if(spell.getCooldown() == 0) {
			return;
		}
		
		if(!cooldowns.containsKey(player)) {
			cooldowns.put(player, new HashMap<Spell, Long>());
		}
		
		cooldowns.get(player).put(spell, System.currentTimeMillis() + spell.getCooldown());
	}
	
	public void increaseCooldown(String player, Spell spell, long ms) {
		if(!cooldowns.containsKey(player)) {
			cooldowns.put(player, new HashMap<Spell, Long>());
			return;
		}
		if(cooldowns.get(player).containsKey(spell)) {
			cooldowns.get(player).put(spell, cooldowns.get(player).get(spell) + ms);
		}
	}
	
	public boolean isOffCooldown(String player, Spell spell) {
		if(spell.getCooldown() == 0) {
			return true;
		}
		
		if(!cooldowns.containsKey(player)) {
			return true;
		}
		
		HashMap<Spell, Long> spellCooldowns = cooldowns.get(player);
		
		if(!spellCooldowns.containsKey(spell) || spellCooldowns.get(spell) < System.currentTimeMillis()) {
			return true;
		}
		
		return false;
	}
	
	public long getRemainingCooldown(String player, Spell spell) {
		if(spell.getCooldown() == 0) {
			return 0;
		}
		
		if(!cooldowns.containsKey(player)) {
			return 0;
		}
		
		HashMap<Spell, Long> spellCooldowns = cooldowns.get(player);
		
		if(!spellCooldowns.containsKey(spell) || spellCooldowns.get(spell) < System.currentTimeMillis()) {
			return 0;
		}
		
		return spellCooldowns.get(spell) - System.currentTimeMillis();
	}
	
	public void notifyCooldown(Player player, Spell spell) {
		player.sendMessage(ChatColor.GOLD + "Spell on cooldown for " + ChatColor.WHITE + Float.toString(Math.round((float) getRemainingCooldown(player.getName(), spell) / 100F) / 10F) + ChatColor.GOLD + "s.");
	}
	
	// Gets the armor scaling value for a player
	public static float getScaling(Player player) {
		float bonus = 0;
		ItemStack[] items = player.getInventory().getArmorContents();
		
		for(ItemStack item : items) {
			if(armor.containsKey(item.getType())) {
				bonus += armor.get(item.getType());
			}
		}
		
		return bonus;
	}
}
