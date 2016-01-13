package me.aqlow.spigot.pvp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.aqlow.spigot.pvp.ai.AIUtil;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;

public class CreatureManager implements Listener {
	public static CreatureManager instance;
	
	// Map of the creature UUIDs to the entities
	private HashMap<UUID, WeakReference<LivingEntity>> creatures = new HashMap<UUID, WeakReference<LivingEntity>>();
	// Map of the creature UUIDs to the owners
	private HashMap<UUID, String> owners = new HashMap<UUID, String>();
	
	public CreatureManager() {
		instance = this;
	}
	
	public String getOwner(UUID uuid) {
		return owners.get(uuid);
	}
	
	public ArrayList<LivingEntity> getEntitiesByOwner(String owner) {
		ArrayList<LivingEntity> leList = new ArrayList<LivingEntity>();
		
		for(UUID uuid : owners.keySet()) {
			String owner2 = owners.get(uuid);
			
			if(owner2.equals(owner)) {
				WeakReference<LivingEntity> entityRef = creatures.get(uuid);
				LivingEntity le = entityRef.get();
				
				if(le != null) {
					leList.add(le);
				}
			}
		}
		
		return leList;
	}
	
	public void removeCreature(UUID creature) {
		creatures.remove(creature);
		String owner = owners.remove(creature);
		
		if(owner != null) {
			Player player = Bukkit.getPlayerExact(owner);

			if(player != null) {
				ItemStack[] contents = player.getInventory().getContents();

				for(int i = 0; i < contents.length; i++) {
					ItemStack item = contents[i];
					if(item == null) continue;
					if(!item.hasItemMeta()) continue;
					if(!item.getItemMeta().hasLore()) continue;
					CreatureLink link = CreatureLink.fromMeta(player.getName(), item);
					if(link == null) continue;

					if(link.getLastInfo().getUUID().equals(creature)) {
						player.getInventory().setItem(i, link.getLastInfo().toItem());
						return;
					}
				}
			}
		}
	}
	
	public void removeCreature(CreatureInfo info) {
		UUID creature = info.getUUID();
		creatures.remove(creature);
		String owner = owners.remove(creature);
		
		if(owner != null) {
			Player player = Bukkit.getPlayerExact(owner);

			if(player != null) {
				ItemStack[] contents = player.getInventory().getContents();

				for(int i = 0; i < contents.length; i++) {
					ItemStack item = contents[i];
					if(item == null) continue;
					if(!item.hasItemMeta()) continue;
					if(!item.getItemMeta().hasLore()) continue;
					CreatureLink link = CreatureLink.fromMeta(player.getName(), item);
					if(link == null) continue;

					if(link.getLastInfo().getUUID().equals(creature)) {
						player.getInventory().setItem(i, info.toItem());
						return;
					}
				}
			}
		}
	}
	
	public void removeCreature(UUID creature, Player player, int slot, CreatureInfo info) {
		creatures.remove(creature);
		owners.remove(creature);
		
		player.getInventory().setItem(slot, info.toItem());
	}
	
	public UUID getCreature(LivingEntity le) {
		ArrayList<UUID> toRemove = new ArrayList<UUID>();
		
		for(UUID uuid : creatures.keySet()) {
			WeakReference<LivingEntity> entityRef = creatures.get(uuid);
			LivingEntity entity = entityRef.get();

			if(entity == null) {
				toRemove.add(uuid);
				continue;
			}

			if(entity.getUniqueId().equals(le.getUniqueId())) {
				for(UUID remove : toRemove) {
					removeCreature(remove);
				}
				return uuid;
			}
			
			if(entity.isDead()) {
				toRemove.add(uuid);
				continue;
			}
		}
		
		for(UUID remove : toRemove) {
			removeCreature(remove);
		}
		return null;
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		UUID uuid = getCreature(event.getEntity());
		
		if(uuid != null) {
			// Prevent duping by killing entities than reviving them
			event.getDrops().clear();
			event.setDroppedExp(0);
			removeCreature(CreatureInfo.fromEntity(event.getEntity()).setUUID(uuid));
		}
	}
	
	@EventHandler
	public void onEntityDespawn(ChunkUnloadEvent event) {
		for(Entity entity : event.getChunk().getEntities()) {
			if(entity instanceof LivingEntity) {
				UUID uuid = getCreature((LivingEntity) entity);
				
				if(uuid != null) {
					// Restores a creature to its egg when its chunk is unloaded
					removeCreature(CreatureInfo.fromEntity((LivingEntity) entity).setUUID(uuid));
					entity.remove();
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// TODO - implement real revive mechanic
		// Temporary revive mechanic - punch the air when holding a creature egg
		if(event.getAction() == Action.LEFT_CLICK_AIR && event.hasItem()) {
			CreatureInfo info = CreatureInfo.fromMeta(event.getItem(), false);
			if(info != null) {
				event.getPlayer().setItemInHand(info.setHealth(1).toItem());
				return;
			}
		}
		
		// Try to spawn a creature on a block face
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			int x = event.getBlockFace().getModX();
			int y = event.getBlockFace().getModY();
			int z = event.getBlockFace().getModZ();
			
			if(doCreatureSpawn(event.getPlayer(), event.getClickedBlock().getLocation().add(x + 0.5, y, z + 0.5))) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if(event.getRightClicked() instanceof Player) {
			return;
		}
		
		// Prevent right-clicking on someone else's entity
		if(event.getRightClicked() instanceof LivingEntity) {
			UUID creature = getCreature((LivingEntity) event.getRightClicked());
			
			if(creature != null) {
				String owner = owners.get(creature);
				
				if(!event.getPlayer().getName().equals(owner)) {
					event.setCancelled(true);
					return;
				}
			}
		}
		
		// Try to pick-up a player-owned creature
		if(event.getRightClicked() instanceof LivingEntity && event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.MONSTER_EGG && doCreaturePickup(event.getPlayer(), (LivingEntity) event.getRightClicked())) {
			event.setCancelled(true);
			return;
		}
		
		// Try to spawn a creature on an entity
		if(doCreatureSpawn(event.getPlayer(), event.getRightClicked().getLocation())) {
			event.setCancelled(true);
			return;
		}
	}
	
	// Reset all broken creature links on player log-in
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		ItemStack[] contents = p.getInventory().getContents();
		
		for(int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if(item == null) continue;
			if(!item.hasItemMeta()) continue;
			if(!item.getItemMeta().hasLore()) continue;
			CreatureLink link = CreatureLink.fromMeta(p.getName(), item);
			if(link == null) continue;
			
			if(!creatures.containsKey(link.getLastInfo().getUUID())) {
				p.getInventory().setItem(i, link.getLastInfo().toItem());
			}
		}
	}
	
	public boolean doCreatureSpawn(Player p, Location loc) {
		ItemStack item = p.getItemInHand();
		
		if(item != null && item.getType() == Material.MONSTER_EGG) {
			if(!item.hasItemMeta()) return false;
			ItemMeta meta = item.getItemMeta();
			if(meta == null || !meta.hasLore()) return false;
			
			CreatureInfo info = CreatureInfo.fromMeta(item, false);
			
			if(info != null) {
				if(info.getHealthPercent() <= 0 || !info.isCooldownOver()) {
					return true;
				}
				
				LivingEntity entity = info.toEntity(loc);
				EntityLiving nmsEntity = ((CraftLivingEntity) entity).getHandle();
				creatures.put(info.getUUID(), new WeakReference<LivingEntity>(entity));
				owners.put(info.getUUID(), p.getName());
				if(nmsEntity instanceof EntityInsentient)  {
					AIUtil.updateSelectors((EntityInsentient) nmsEntity);
				}
				info = info.removeItems();
				
				p.getInventory().setItem(p.getInventory().getHeldItemSlot(), new CreatureLink(p.getName(), info).toItem());
				return true;
			}
		}
		
		return false;
	}
	
	public boolean doCreaturePickup(Player p, LivingEntity e) {
		ItemStack[] contents = p.getInventory().getContents();
		
		for(int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if(item == null) continue;
			if(!item.hasItemMeta()) continue;
			if(!item.getItemMeta().hasLore()) continue;
			CreatureLink link = CreatureLink.fromMeta(p.getName(), item);
			if(link == null) continue;
			
			if(creatures.containsKey(link.getLastInfo().getUUID())) {
				WeakReference<LivingEntity> entityRef = creatures.get(link.getLastInfo().getUUID());
				LivingEntity entity = entityRef.get();

				if(entity == null || entity.isDead()) {
					removeCreature(link.getLastInfo().getUUID(), p, i, link.getLastInfo().kill());
					continue;
				}

				if(entity.getUniqueId().equals(e.getUniqueId())) {
					if(link.getLastInfo().isCooldownOver()) {
						return true;
					}
					
					CreatureInfo info = CreatureInfo.fromEntity(e);
					e.remove();
					removeCreature(info.getUUID(), p, i, info);

					return true;
				}
			}
		}
		
		return false;
	}
}
