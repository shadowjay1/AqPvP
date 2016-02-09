package me.aqlow.spigot.pvp;

import static com.comphenix.protocol.PacketType.Play.Server.ANIMATION;
import static com.comphenix.protocol.PacketType.Play.Server.ATTACH_ENTITY;
import static com.comphenix.protocol.PacketType.Play.Server.BED;
import static com.comphenix.protocol.PacketType.Play.Server.COLLECT;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_DESTROY;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_EFFECT;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_HEAD_ROTATION;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_LOOK;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_METADATA;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_MOVE_LOOK;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_STATUS;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_TELEPORT;
import static com.comphenix.protocol.PacketType.Play.Server.ENTITY_VELOCITY;
import static com.comphenix.protocol.PacketType.Play.Server.NAMED_ENTITY_SPAWN;
import static com.comphenix.protocol.PacketType.Play.Server.REL_ENTITY_MOVE;
import static com.comphenix.protocol.PacketType.Play.Server.REMOVE_ENTITY_EFFECT;
import static com.comphenix.protocol.PacketType.Play.Server.UPDATE_ATTRIBUTES;
import static com.comphenix.protocol.PacketType.Play.Server.UPDATE_ENTITY_NBT;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import me.aqlow.spigot.pvp.util.AqUtil;
import me.aqlow.spigot.pvp.util.PlayerMap;
import me.aqlow.spigot.pvp.util.constraint.AllConstraint;
import me.aqlow.spigot.pvp.util.constraint.LoreConstraint;
import me.aqlow.spigot.pvp.util.constraint.TypeConstraint;
import net.minecraft.server.v1_8_R3.EntityPlayer;

public class SoulManager implements Listener {
	private AllConstraint soulShifter = new AllConstraint();
	private PlayerMap<Soul> map = new PlayerMap<Soul>();

	public SoulManager() {
		soulShifter.add(new TypeConstraint(Material.GOLD_SWORD));
		soulShifter.add(new LoreConstraint(new String[] { "Soul-shifter" }));

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener() {
			// Use Bed, Animation, Collect Item, Entity Velocity, Destroy Entities, Entity,
			// Entity Relative Move, Entity Look, Entity Look And Relative Move, Entity Teleport,
			// Entity Head Look, Entity Status, Attach Entity, Entity Metadata, Entity Effect,
			// Remove Entity Effect, Entity Properties
			private ListeningWhitelist sending = ListeningWhitelist.newBuilder().types(
					NAMED_ENTITY_SPAWN, BED, ANIMATION, COLLECT, ENTITY_VELOCITY, ENTITY_DESTROY,
					ENTITY, REL_ENTITY_MOVE, ENTITY_LOOK, ENTITY_MOVE_LOOK, ENTITY_TELEPORT, ENTITY_HEAD_ROTATION,
					ENTITY_STATUS, ATTACH_ENTITY, ENTITY_METADATA, ENTITY_EFFECT, REMOVE_ENTITY_EFFECT,
					UPDATE_ATTRIBUTES, UPDATE_ENTITY_NBT
					).build();

			@Override
			public Plugin getPlugin() {
				return AqPvP.instance;
			}

			@Override
			public ListeningWhitelist getReceivingWhitelist() {
				return ListeningWhitelist.EMPTY_WHITELIST;
			}

			@Override
			public ListeningWhitelist getSendingWhitelist() {
				return sending;
			}

			@Override
			public void onPacketReceiving(PacketEvent event) {}

			@Override
			public void onPacketSending(PacketEvent event) {
				//if(true) return;
				
				if(event.getPacketType() == ATTACH_ENTITY) {
					filterEntityId(event.getPlayer(), event.getPacket().getIntegers(), 1, 2);
				}
				else if(event.getPacketType() == ENTITY || event.getPacketType() == REL_ENTITY_MOVE || 
						event.getPacketType() == ENTITY_LOOK || event.getPacketType() == ENTITY_MOVE_LOOK || 
						event.getPacketType() == ENTITY_TELEPORT || event.getPacketType() == ENTITY_HEAD_ROTATION ||
						event.getPacketType() == ENTITY_STATUS || event.getPacketType() == ENTITY_METADATA || 
						event.getPacketType() == ENTITY_EFFECT || event.getPacketType() == REMOVE_ENTITY_EFFECT || 
						event.getPacketType() == UPDATE_ATTRIBUTES || event.getPacketType() == UPDATE_ENTITY_NBT ||
						event.getPacketType() == ENTITY_VELOCITY) {
					filterEntityId(event.getPlayer(), event.getPacket().getIntegers(), 0);
				}
				else if(event.getPacketType() == ENTITY_DESTROY) {
					filterEntityIds(event.getPlayer(), event.getPacket().getIntegerArrays(), 0);
				}
				else if(event.getPacketType() == COLLECT) {
					filterEntityId(event.getPlayer(), event.getPacket().getIntegers(), 1);
				}
				else if(event.getPacketType() == BED || event.getPacketType() == ANIMATION) {
					//event.setCancelled(true);
				}
				else if(event.getPacketType() == NAMED_ENTITY_SPAWN) {
					UUID uuid = event.getPacket().getSpecificModifier(UUID.class).read(0);
					Player player = Bukkit.getPlayer(uuid);
					Soul soul = map.get(player);

					if(soul != null) {
						event.setCancelled(true);
						try {
							ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), createSpawnPacket(player, soul.type, soul.id, soul.data));
						}
						catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}

			}

		});
	}

	private void filterEntityId(Player p, StructureModifier<Integer> modifier, int... indices) {
		for(int i : indices) {
			modifier.write(i, filter(p, modifier.read(i)));
		}
	}

	private void filterEntityIds(Player p, StructureModifier<int[]> modifier, int... indices) {
		for(int i : indices) {
			int[] array = modifier.read(i);
			for(int j = 0; j < array.length; j++) {
				array[j] = filter(p, array[j]);
			}
			modifier.write(i, array);
		}
	}

	private int filter(Player player, int id) {
		for(Player p : map.keySet()) {
			if(p.getEntityId() == id) {
				if(player != null && p.getUniqueId().equals(player.getUniqueId())) return id;
				return map.get(p).id;
			}
		}

		return id;
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if(onPlayerClick(event.getPlayer(), event.getRightClicked())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player) {
			if(onPlayerClick((Player) event.getDamager(), event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	//@SuppressWarnings("deprecation")
	public boolean onPlayerClick(final Player p, final Entity e) {
		if(soulShifter.matches(p.getItemInHand())) {
			try {
				int id = AqUtil.generateEntityId();
				PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(ENTITY_DESTROY);
				packet.getIntegerArrays().write(0, new int[] { filter(null, p.getEntityId()) });
				ProtocolLibrary.getProtocolManager().broadcastServerPacket(packet, p, false);
				WrappedDataWatcher data = WrappedDataWatcher.getEntityWatcher(e);
				data.setObject(2, p.getName());
				map.put(p, new Soul(e.getType(), id, data));
				p.teleport(e.getLocation());
				e.remove();
				ProtocolLibrary.getProtocolManager().broadcastServerPacket(createSpawnPacket(p, e.getType(), id, data), p, false);
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}

			return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	private PacketContainer createSpawnPacket(Player player, EntityType type, int id, WrappedDataWatcher data) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
		packet.getIntegers().write(0, id);
		packet.getIntegers().write(1, (int) type.getTypeId());
		packet.getIntegers().write(2, (int) Math.floor(player.getLocation().getX() * 32));
		packet.getIntegers().write(3, (int) Math.floor(player.getLocation().getY() * 32));
		packet.getIntegers().write(4, (int) Math.floor(player.getLocation().getZ() * 32));
		packet.getIntegers().write(5, 0);
		packet.getIntegers().write(6, 0);
		packet.getIntegers().write(7, 0);
		packet.getBytes().write(0, (byte) (nmsPlayer.yaw * 256F / 360F));
		packet.getBytes().write(1, (byte) (nmsPlayer.pitch * 256F / 360F));
		packet.getBytes().write(2, (byte) (nmsPlayer.aK * 256F / 360F));
		packet.getDataWatcherModifier().write(0, data);

		return packet;
	}

	private class Soul {
		private EntityType type;
		private int id;
		private WrappedDataWatcher data;

		public Soul(EntityType type, int id, WrappedDataWatcher data) {
			this.type = type;
			this.id = id;
			this.data = data;
		}
	}
}
