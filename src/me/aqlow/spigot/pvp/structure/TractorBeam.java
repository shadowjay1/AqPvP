package me.aqlow.spigot.pvp.structure;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreature;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;

import me.aqlow.spigot.pvp.ai.AIUtil;
import me.aqlow.spigot.pvp.spells.SpellProjectile;
import me.aqlow.spigot.pvp.spells.SpellProjectile.SpellProjectileHandler;
import me.aqlow.spigot.pvp.util.PseudoEntityWrapper;

public class TractorBeam implements Structure, Listener {
	private HashMap<Location, BeamInfo> tractorBeams = new HashMap<Location, BeamInfo>();
	
	private static final long COOLDOWN = 1000;

	@Override
	public boolean isStructure(Block block) {
		if(block.getType() == Material.FURNACE) {
			Furnace furnace = (Furnace) block.getState();
			ItemStack smelting = furnace.getInventory().getSmelting();
			if(smelting != null && smelting.getType() == Material.EYE_OF_ENDER) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean activateStructure(Block block) {
		if(tractorBeams.containsKey(block.getLocation())) return false;

		Creature creature = block.getWorld().spawn(block.getLocation().add(0.5, 1, 0.5), Blaze.class);
		creature.setRemoveWhenFarAway(false);
		AIUtil.clearGoalSelector((((CraftCreature) creature).getHandle()));
		AIUtil.clearTargetSelector((((CraftCreature) creature).getHandle()));
		tractorBeams.put(block.getLocation(), new BeamInfo(creature, block.getLocation()));

		return true;
	}

	@Override
	public void deactivateStructures() {
		for(BeamInfo info : tractorBeams.values()) {
			info.shooter.remove();
		}

		tractorBeams.clear();
	}

	@Override
	public void onTick() {
		ArrayList<Location> toRemove = new ArrayList<Location>();

		for(Location loc : tractorBeams.keySet()) {
			if(!isStructure(loc.getBlock())) {
				toRemove.add(loc);
			}
			else {
				BeamInfo info = tractorBeams.get(loc);
				Creature creature = info.shooter;
				//System.out.println(creature.getLocation().getYaw());
				creature.teleport(loc.clone().add(0.5, 1, 0.5).setDirection(creature.getLocation().getDirection()));

				createParticle(loc.clone().add(0, 1, 0));
				createParticle(loc.clone().add(1, 1, 0));
				createParticle(loc.clone().add(0, 1, 1));
				createParticle(loc.clone().add(1, 1, 1));

				attemptProjectile(info);
			}
		}

		for(Location loc : toRemove) {
			BeamInfo info = tractorBeams.remove(loc);
			info.shooter.remove();
		}
	}
	
	private void attemptProjectile(BeamInfo info) {
		Block block = info.location.getBlock();
		Furnace furnace = (Furnace) block.getState();
		ItemStack fuel = furnace.getInventory().getFuel();
		if(info.fireVelocity != null && System.currentTimeMillis() > info.lastFired + COOLDOWN) {
			if(isFuel(furnace.getInventory().getFuel())) {
				fuel.setAmount(fuel.getAmount() - 1);
				furnace.getInventory().setFuel(fuel.getAmount() < 1 ? null : fuel);
				fireProjectile(info.shooter.getEyeLocation().add(info.fireVelocity.normalize()), info.fireVelocity);
				info.lastFired = System.currentTimeMillis();
				info.fireVelocity = null;
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void createParticle(Location loc) {
		loc.getWorld().spigot().playEffect(loc, Effect.TILE_DUST, Material.FIRE.getId(), 0, 0, 0.3F, 0, 1, 0, 50);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		for(BeamInfo info : tractorBeams.values()) {
			if(info.shooter.equals(event.getEntity())) {
				event.setCancelled(true);
				break;
			}
		}
	}

	@EventHandler
	public void onInventoryClick(final InventoryClickEvent event) {
		if(event.getClickedInventory() instanceof FurnaceInventory) {
			if(!isFuel(event.getCursor())) return;
			if(event.getRawSlot() != 1) return; // only continue if the player clicked the fuel slot

			//event.setCancelled(true);
			event.setResult(Result.DENY);
			if(event.getClick() == ClickType.LEFT) {
				if(event.getCurrentItem().isSimilar(event.getCursor())) {
					int shift = event.getCurrentItem().getMaxStackSize() - event.getCurrentItem().getAmount();
					shift = Math.min(shift, event.getCursor().getAmount());
					
					ItemStack newCurrentItem = event.getCurrentItem().clone();
					newCurrentItem.setAmount(newCurrentItem.getAmount() + shift);
					event.getView().setItem(1, newCurrentItem);

					ItemStack newCursor = event.getCursor().clone();
					newCursor.setAmount(newCursor.getAmount() - shift);
					if(newCursor.getAmount() < 1) newCursor = null;
					event.getView().setCursor(newCursor);
				}
				else {
					ItemStack oldCursor = event.getCursor().clone();
					event.getView().setCursor(event.getCurrentItem());
					event.getView().setItem(1, oldCursor);
				}
			}
			else if(event.getClick() == ClickType.RIGHT) {
				if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
					ItemStack newCurrentItem = event.getCursor().clone();
					newCurrentItem.setAmount(1);
					event.getView().setItem(1, newCurrentItem);

					ItemStack newCursor = event.getCursor().clone();
					newCursor.setAmount(newCursor.getAmount() - 1);
					if(newCursor.getAmount() < 1) newCursor = null;
					event.getView().setCursor(newCursor);
				}
				else if(event.getCurrentItem().isSimilar(event.getCursor())) {
					ItemStack newCurrentItem = event.getCurrentItem().clone();
					if(newCurrentItem.getAmount() < newCurrentItem.getMaxStackSize()) {
						newCurrentItem.setAmount(newCurrentItem.getAmount() + 1);
						event.getView().setItem(1, newCurrentItem);

						ItemStack newCursor = event.getCursor().clone();
						newCursor.setAmount(newCursor.getAmount() - 1);
						if(newCursor.getAmount() < 1) newCursor = null;
						event.getView().setCursor(newCursor);
					}
				}
			}
		}
	}

	private boolean isFuel(ItemStack item) {
		if(item != null && item.getType() == Material.RABBIT_FOOT) {
			return true;
		}

		return false;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getPlayer().getVehicle() != null) {
			for(BeamInfo info : tractorBeams.values()) {
				if(info.shooter.equals(event.getPlayer().getVehicle())) {
					Vector direction = event.getPlayer().getLocation().getDirection();
					Vector velocity = direction.clone().multiply(3);
					info.fireVelocity = velocity;
					attemptProjectile(info);
					Location loc = info.shooter.getLocation().setDirection(direction);
					info.shooter.teleport(loc);
					byte yaw = (byte) (loc.getYaw() * 256F / 360F);
					byte pitch = (byte) (loc.getPitch() * 256F / 360F);
					PacketContainer look = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_LOOK);
					look.getIntegers().write(0, info.shooter.getEntityId());
					look.getBytes().write(0, yaw);
					look.getBytes().write(1, pitch);
					look.getBooleans().write(0, info.shooter.isOnGround());
					PacketContainer rotate = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
					rotate.getIntegers().write(0, info.shooter.getEntityId());
					rotate.getBytes().write(0, yaw);
					ProtocolLibrary.getProtocolManager().broadcastServerPacket(rotate, info.shooter, false);
					ProtocolLibrary.getProtocolManager().broadcastServerPacket(look, info.shooter, false);
					break;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		for(BeamInfo info : tractorBeams.values()) {
			if(info.shooter.equals(event.getRightClicked())) {
				info.shooter.setPassenger(event.getPlayer());
				break;
			}
		}
	}

	private void fireProjectile(Location loc, Vector velocity) {
		SpellProjectile proj = new SpellProjectile(new PseudoEntityWrapper(loc, velocity, 50), 0.5);
		proj.setHandler(new SpellProjectileHandler() {
			@Override
			public boolean onHit(SpellProjectile proj, Entity e) {
				if(!(e instanceof LivingEntity) || e.isDead()) return false;
				e.getWorld().strikeLightning(e.getLocation());
				proj.getEntity().remove();
				return true;
			}

			@SuppressWarnings("deprecation")
			@Override
			public void onTick(SpellProjectile proj) {
				double r = Math.random();
				Location loc;
				if(proj.getLastLocation() != null) {
					loc = proj.getEntity().getLocation().clone().multiply(r).add(proj.getLastLocation().multiply(1 - r));
				}
				else {
					loc = proj.getEntity().getLocation();
				}
				proj.getEntity().getLocation().getWorld().spigot().playEffect(loc, Effect.TILE_DUST, Material.ANVIL.getId(), 0, 0.0F, 0.0F, 0.0F, 0.03F, 5, 50);
			}
		});
		proj.activate();
	}

	private class BeamInfo {
		private Creature shooter;
		private Location location;
		private long lastFired = Long.MIN_VALUE;
		private Vector fireVelocity = null;

		public BeamInfo(Creature blaze, Location loc) {
			this.shooter = blaze;
			this.location = loc;
		}
	}
}
