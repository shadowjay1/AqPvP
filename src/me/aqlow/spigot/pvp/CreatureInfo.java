package me.aqlow.spigot.pvp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtWrapper;
import com.comphenix.protocol.wrappers.nbt.io.NbtTextSerializer;

import me.aqlow.spigot.pvp.util.AqUtil;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public final class CreatureInfo {
	// cooldown to prevent accidentally double clicking with an egg to pick-up+respawn again
	private static final long EGG_COOLDOWN = 100;
	
	private final EntityType type;
	private final double healthPercent;
	private final UUID uuid; // a UUID to identify the creature
	private final String name;
	private final String serialized; // the serialized NBT data
	private final long lastUsed; // used with EGG_COOLDOWN
	
	public CreatureInfo(EntityType type, double healthPercent, String name, String serialized) {
		this(type, healthPercent, UUID.randomUUID(), name, serialized, System.currentTimeMillis());
	}
	
	private CreatureInfo(EntityType type, double healthPercent, UUID uuid, String name, String serialized, long lastUsed) {
		this.type = type;
		this.healthPercent = healthPercent;
		this.uuid = uuid;
		this.name = name;
		this.serialized = serialized;
		this.lastUsed = lastUsed;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public CreatureInfo setUUID(UUID uuid) {
		return new CreatureInfo(this.type, this.healthPercent, uuid, this.name, this.serialized, this.lastUsed);
	}
	
	public String getName() {
		return name;
	}
	
	public double getHealthPercent() {
		return healthPercent;
	}
	
	public CreatureInfo kill() {
		return new CreatureInfo(this.type, 0, this.uuid, this.name, this.serialized, System.currentTimeMillis());
	}
	
	public CreatureInfo setHealth(double healthPercent) {
		return new CreatureInfo(this.type, healthPercent, this.uuid, this.name, this.serialized, System.currentTimeMillis());
	}
	
	public boolean isCooldownOver() {
		return System.currentTimeMillis() > this.lastUsed + EGG_COOLDOWN;
	}
	
	// Remove any NBT attributes that may result in a dupe glitch
	public CreatureInfo removeItems() {
		String serialized = "";
		
		try {
			NbtCompound compound = NbtTextSerializer.DEFAULT.deserializeCompound(this.serialized);
			compound.remove("ArmorItem");
			compound.remove("SaddleItem");
			compound.remove("Items");
			serialized = NbtTextSerializer.DEFAULT.serialize(compound);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return new CreatureInfo(this.type, this.healthPercent, this.uuid, this.name, serialized, System.currentTimeMillis());
	}
	
	// Creates an entity from the creature info
	@SuppressWarnings("rawtypes")
	public LivingEntity toEntity(Location loc) {
		LivingEntity entity = (LivingEntity) loc.getWorld().spawn(loc, type.getEntityClass());
		
		if(serialized != null && !serialized.isEmpty()) {
			try {
				NbtCompound nbtWrapper = NbtTextSerializer.DEFAULT.deserializeCompound(serialized);
				
				((CraftLivingEntity) entity).getHandle().a((NBTTagCompound) ((NbtWrapper) nbtWrapper).getHandle());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("ERROR: no serialized");
		}
		
		entity.teleport(loc);
		entity.setHealth(entity.getMaxHealth() * healthPercent);
		if(this.name != null) entity.setCustomName(this.name);
		
		return entity;
	}
	
	// Creates a creature info from an entity
	public static CreatureInfo fromEntity(LivingEntity entity) {
		NBTTagCompound nbt = new NBTTagCompound();
		((CraftLivingEntity) entity).getHandle().b(nbt);
		NbtCompound compound = NbtFactory.fromNMSCompound(nbt);
		String serialized = NbtTextSerializer.DEFAULT.serialize(compound);
		
		return new CreatureInfo(entity.getType(), entity.getHealth() / entity.getMaxHealth(), entity.getCustomName(), serialized);
	}
	
	// Creates a short description of the creature info
	@SuppressWarnings("deprecation")
	public List<String> toLore() {
		String line1 = type.getName();
		String line2 = String.format("HP: %.1f", healthPercent * 100) + "%";

		return Arrays.asList(line1, line2);
	}
	
	// Creates an ItemStack representation of the creature info
	@SuppressWarnings("deprecation")
	public ItemStack toItem() {
		ItemStack newItem = new ItemStack(Material.MONSTER_EGG);
		newItem.setDurability(type.getTypeId());
		ItemMeta meta = newItem.getItemMeta();
		meta.setDisplayName(this.name);
		meta.setLore(this.toLore());
		newItem.setItemMeta(meta);
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:type", type.toString());
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:health", Double.toString(healthPercent));
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:uuid", uuid.toString());
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:serialized", serialized);
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:lastused", Long.toString(lastUsed));
		
		return newItem;
	}
	
	// Gets a creature info from an ItemStack or null if a creature info can't be found
	// allowLink - determines whether it should return the creature info from a creature link or null
	public static CreatureInfo fromMeta(ItemStack item, boolean allowLink) {
		String[] values = AqUtil.loadAllOrNoneItemData(item, new String[] {
			"aqpvp:creature:type",
			"aqpvp:creature:health",
			"aqpvp:creature:uuid",
			"aqpvp:creature:serialized",
			"aqpvp:creature:lastused"
		});
		if(values == null) return null;
		String link = AqUtil.loadItemData(item, "aqpvp:creature:linked");
		if(link != null && !allowLink) return null; // return null if it's a link instead of an info
		
		try {
			String name = null;
			if(item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
				name = item.getItemMeta().getDisplayName();
			}
			
			EntityType type = EntityType.valueOf(values[0]);
			UUID uuid = UUID.fromString(values[2]);
			String serialized = values[3];
			if(LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
				double healthPercent = Math.max(0, Math.min(1, Double.parseDouble(values[1])));
				
				return new CreatureInfo(type, healthPercent, uuid, name, serialized, Long.parseLong(values[4]));
			}
		}
		catch(IllegalArgumentException e) {}
		
		return null;
	}
}
