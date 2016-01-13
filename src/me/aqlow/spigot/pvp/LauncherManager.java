package me.aqlow.spigot.pvp;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftThrownPotion;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;

import me.aqlow.spigot.pvp.runes.Rune;
import me.aqlow.spigot.pvp.util.AqUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class LauncherManager implements Listener {
	public static int COOLDOWN_UPDATE_FREQUENCY = 5;
	public static float POTION_SPEED = 2.0F;
	private static HashSet<Player> toUpdate = new HashSet<Player>();
	public static double POTION_RADIUS = 0;
	public static double DAMAGE_3_MULTIPLIER = 0.8F;
	public static String INTENSITY_EQUATION = "";
	// air_time, distance_sq
	private static Expression intensityEquation = null;
	
	private static int lastTask = -1;
	
	public LauncherManager() {
		
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN_UPDATE_FREQUENCY = config.getInt("launcher.cooldown-update-frequency", 5);
		POTION_SPEED = (float) config.getDouble("launcher.potion-speed", 2);
		POTION_RADIUS = config.getDouble("launcher.potion-radius", 5);
		INTENSITY_EQUATION = config.getString("launcher.intensity-expr", "1");
		DAMAGE_3_MULTIPLIER = config.getDouble("launcher.damage-3-multiplier", 0.8D);
		intensityEquation = new ExpressionBuilder(INTENSITY_EQUATION).variables("air_time", "distance_sq").build();
		
		if(lastTask > -1) Bukkit.getScheduler().cancelTask(lastTask);
		lastTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			@Override
			public void run() {
				Iterator<Player> iterator = toUpdate.iterator();
				
				while(iterator.hasNext()) {
					Player player = iterator.next();
					ItemStack item = player.getItemInHand();
					CustomWeapon type = CustomWeapon.getType(item);
					if(type == null) {
						iterator.remove();
						continue;
					}
					
					try {
						long previous = Long.parseLong(AqUtil.loadItemData(item, "aqpvp:reloadStart"));
						double percent = (type.getChargeTime(player) - (double) (System.currentTimeMillis() - previous)) / type.getChargeTime(player);
						if(percent > 0 || true) {
							item.setDurability((short) (percent * 33));
							//player.setItemInHand(item);
							PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT);
							packet.getIntegers().write(0, 0); // window id = player inventory (0)
							packet.getShorts().write(0, (short) player.getInventory().getHeldItemSlot()); // item slot
							packet.getItemModifier().write(0, item);
							return;
						}
					}
					catch(Exception e) {}
				}
			}
		}, 0, COOLDOWN_UPDATE_FREQUENCY);
	}
	
	public void add(Player p) {
		toUpdate.add(p);
	}
	
	public ItemStack reset(ItemStack item) {
		item = AqUtil.saveItemData(item, "aqpvp:reloadStart", Long.toString(System.currentTimeMillis()));
		item.setDurability((short) 33);
		return item;
	}
	
	public void remove(Player p) {
		toUpdate.remove(p);
	}
	
	@EventHandler
	public void onPotionThrow(PlayerInteractEvent event) {
		if(event.hasItem() && event.getItem().getType() == Material.POTION) {
			if(event.getItem().hasItemMeta()) {
				ItemMeta meta = event.getItem().getItemMeta();

				if(meta.hasLore() && meta.getLore().size() >= 1 && meta.getLore().get(0).equals("(Launcher-only)")) {
					event.setUseItemInHand(Result.DENY);
					event.getPlayer().setItemInHand(event.getPlayer().getItemInHand());
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack itemInHand = event.getPlayer().getItemInHand();
		
		if(CustomWeapon.getType(itemInHand) != null) {
			toUpdate.add(event.getPlayer());
		}
		
		if((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.hasItem() && event.getItem().getType() == Material.GOLD_HOE) {
			event.setCancelled(true);
			
			ItemStack hoe = event.getItem();
			try {
				long previous = Long.parseLong(AqUtil.loadItemData(hoe, "aqpvp:reloadStart"));
				double percent = (CustomWeapon.LAUNCHER.getChargeTime(event.getPlayer()) - (double) (System.currentTimeMillis() - previous)) / CustomWeapon.LAUNCHER.getChargeTime(event.getPlayer());
				if(percent > 0) {
					hoe.setDurability((short) (percent * 33));
					return;
				}
			}
			catch(Exception e) {}
			
			PlayerInventory inv = event.getPlayer().getInventory();
			for(int i = 0; i < inv.getContents().length; i++) {
				ItemStack item = inv.getContents()[i];
				
				if(item != null && item.getType() == Material.POTION && (item.getDurability() & 0x4000) == 0x4000) {
					CraftThrownPotion thrown = (CraftThrownPotion) CraftEntity.getEntity((CraftServer) Bukkit.getServer(), new net.minecraft.server.v1_8_R3.EntityPotion(((CraftWorld) event.getPlayer().getWorld()).getHandle(), ((CraftPlayer) event.getPlayer()).getHandle(), CraftItemStack.asNMSCopy(item)));
					((CraftWorld) thrown.getWorld()).addEntity(thrown.getHandle(), SpawnReason.CUSTOM);
					float multiplier = RuneManager.instance.hasRune(event.getPlayer(), Rune.POWER_LAUNCHER) ? RuneManager.POWER_LAUNCHER_MULTIPLIER : 1;
					thrown.setVelocity(event.getPlayer().getLocation().getDirection().multiply(POTION_SPEED * multiplier));
					thrown.setMetadata("aqpvp:launcher-potion", new FixedMetadataValue(AqPvP.instance, true));
					if(item.getAmount() > 1) {
						item.setAmount(item.getAmount() - 1);
						inv.setItem(i, item);
					}
					else {
						inv.removeItem(item);
					}
					
					event.getPlayer().setItemInHand(AqUtil.saveItemData(hoe, "aqpvp:reloadStart", Long.toString(System.currentTimeMillis())));
					
					return;
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		ItemStack item = event.getPlayer().getItemInHand();
		
		if(CustomWeapon.getType(item) != null) {
			toUpdate.add(event.getPlayer());
			event.getPlayer().setItemInHand(reset(item));
		}
	}
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if(CustomWeapon.getType(event.getItem().getItemStack()) != null) {
			toUpdate.add(event.getPlayer());
			event.getItem().setItemStack(reset(event.getItem().getItemStack()));
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		
		if(event.getCurrentItem() != null && CustomWeapon.getType(event.getCurrentItem()) != null) {
			toUpdate.add((Player) event.getWhoClicked());
			event.setCurrentItem(reset(event.getCurrentItem()));
		}
		
		if(event.getCursor() != null && CustomWeapon.getType(event.getCursor()) != null) {
			toUpdate.add((Player) event.getWhoClicked());
			event.setCursor(reset(event.getCursor()));
		}
	}
	
	@EventHandler
	public void onPlayerMoveItem(InventoryMoveItemEvent event) {
		if(event.getDestination() instanceof PlayerInventory && CustomWeapon.getType(event.getItem()) != null) {
			toUpdate.add((Player) ((PlayerInventory) event.getDestination()).getHolder());
			event.setItem(reset(event.getItem()));
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		toUpdate.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerSwitchItems(PlayerItemHeldEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
		ItemStack old = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
		
		if(CustomWeapon.getType(item) != null) {
			toUpdate.add(event.getPlayer());
			event.getPlayer().getInventory().setItem(event.getNewSlot(), reset(item));
		}
		
		if(CustomWeapon.getType(old) != null) {
			event.getPlayer().getInventory().setItem(event.getPreviousSlot(), reset(old));
		}
	}
	
	@EventHandler
	public void onPotionSplash(PotionSplashEvent event) {
		if(event.getPotion().hasMetadata("aqpvp:launcher-potion")) {
			for(LivingEntity entity : event.getAffectedEntities()) {
				event.setIntensity(entity, 0);
			}
			
			intensityEquation.setVariable("air_time", event.getEntity().getTicksLived());
			
			for(LivingEntity entity : AqUtil.getEntitiesInRadius(event.getPotion().getLocation(), POTION_RADIUS, LivingEntity.class)) {
				intensityEquation.setVariable("distance_sq", event.getPotion().getLocation().distanceSquared(entity.getLocation()));
				event.setIntensity(entity, intensityEquation.evaluate());
			}
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Potion) {
			Potion potion = (Potion) event.getDamager();
			
			for(PotionEffect effect : potion.getEffects()) {
				// TODO - make sure this works
				if(effect.getType() == PotionEffectType.HARM && effect.getAmplifier() == 2) {
					event.setDamage(event.getDamage() * DAMAGE_3_MULTIPLIER);
				}
			}
		}
	}
}
