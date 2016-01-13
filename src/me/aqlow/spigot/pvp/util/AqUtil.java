package me.aqlow.spigot.pvp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public class AqUtil {
	public static final char ENCODING_CHAR = ChatColor.COLOR_CHAR;
	public static final String ENCODING_SEPARATOR = "" + ENCODING_CHAR + ENCODING_CHAR;
	public static final char ESCAPE_CHAR = '~';
	public static final HashMap<Character, Character> ESCAPED_R = new HashMap<Character, Character>();
	public static final HashMap<Character, Character> ESCAPED = new HashMap<Character, Character>();
	
	static {
		ESCAPED.put('~', '~');
		ESCAPED_R.put('~', '~');
		ESCAPED.put('\n', 'n');
		ESCAPED_R.put('n', '\n');
		ESCAPED.put(ENCODING_CHAR, '%');
		ESCAPED_R.put('%', ENCODING_CHAR);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Entity> ArrayList<T> getEntitiesInRadius(Location loc, double radius, Class<T> type) {
		// find involved chunks
		int chunkX = loc.getChunk().getX();
		int chunkZ = loc.getChunk().getZ();
		int chunkRadius = (int) Math.ceil(radius / 16);
		int chunkRadiusSquare = chunkRadius * chunkRadius;
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		for(int x = -chunkRadius; x <= chunkRadius; x++) {
			for(int z = -chunkRadius; z <= chunkRadius; z++) {
				if((x * x) + (z * z) <= chunkRadiusSquare) {
					chunks.add(loc.getWorld().getChunkAt(chunkX + x, chunkZ + z));
				}
			}
		}
		
		// check all entities in chunks
		ArrayList<T> entities = new ArrayList<T>();
		
		for(Chunk chunk : chunks) {
			for(Entity entity : chunk.getEntities()) {
				if(type.isAssignableFrom(entity.getClass()) && entity.getLocation().distance(loc) <= radius) {
					entities.add((T) entity);
				}
			}
		}
			
		return entities;
	}
	
	public static ArrayList<Entity> getEntitiesInRadius(Location loc, double radius) {
		// find involved chunks
		int chunkX = loc.getChunk().getX();
		int chunkZ = loc.getChunk().getZ();
		int chunkRadius = (int) Math.ceil(radius / 16);
		int chunkRadiusSquare = chunkRadius * chunkRadius;
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		for(int x = -chunkRadius; x <= chunkRadius; x++) {
			for(int z = -chunkRadius; z <= chunkRadius; z++) {
				if((x * x) + (z * z) <= chunkRadiusSquare) {
					chunks.add(loc.getWorld().getChunkAt(chunkX + x, chunkZ + z));
				}
			}
		}
		
		// check all entities in chunks
		ArrayList<Entity> entities = new ArrayList<Entity>();
		
		for(Chunk chunk : chunks) {
			for(Entity entity : chunk.getEntities()) {
				if(entity.getLocation().distance(loc) <= radius) {
					entities.add(entity);
				}
			}
		}
			
		return entities;
	}
	
	public static String[] decodeString(String s) {
		ArrayList<String> array = new ArrayList<String>();
		String current = "";
		
		for(int i = 1; i < s.length(); i += 2) {
			if(s.charAt(i) == ENCODING_CHAR) {
				array.add(current);
				current = "";
				
				if((i + 3) < s.length() && s.charAt(i + 3) != ENCODING_CHAR) {
					array.add(s.substring(i + 1));
					return array.toArray(new String[0]);
				}
				
				continue;
			}
			else if(s.charAt(i) == ESCAPE_CHAR) {
				if((i + 2) < s.length()) {
					i += 2;
					
					if(ESCAPED_R.containsKey(s.charAt(i))) {
						current += ESCAPED_R.get(s.charAt(i));
					}
				}
			}
			else {
				current += s.charAt(i);
			}
		}
		
		array.add("");
		
		return array.toArray(new String[0]);
	}
	
	public static String encodeStrings(String[] array, String suffix) {
		String encoded = "";
		
		for(String s : array) {
			encoded += encodeString(s) + ENCODING_SEPARATOR;
		}
		
		if(!suffix.startsWith(Character.toString(ENCODING_CHAR))) {
			suffix = ChatColor.WHITE + suffix;
		}
		
		return encoded + suffix;
	}
	
	public static String encodeString(String s) {
		String encoded = "";
		
		for(int i = 0; i < s.length(); i++) {
			if(ESCAPED.containsKey(s.charAt(i))) {
				encoded += Character.toString(ENCODING_CHAR) + ESCAPE_CHAR;
				encoded += Character.toString(ENCODING_CHAR) + ESCAPED.get(s.charAt(i));
			}
			else {
				encoded += Character.toString(ENCODING_CHAR) + s.charAt(i);
			}
		}
		
		return encoded;
	}
	
	// Bukkit limits the length of strings that can be read from lore to 1024 for some reason
	// When reading from the NBT, the full strings are preserved
	public static List<String> readLore(ItemStack stack) {
		NbtCompound compound = (NbtCompound) NbtFactory.fromItemTag(stack);
		NbtCompound display = compound.getCompound("display");
		if(display != null) {
			NbtList<String> lore = display.getList("Lore");
			
			if(lore != null) {
				ArrayList<String> loreList = new ArrayList<String>();
				
				for(int i = 0; i < lore.size(); i++) {
					loreList.add(lore.getValue(i));
				}
				
				return loreList;
			}
		}
		
		return null;
	}
	
	public static ItemStack saveItemData(ItemStack item, String key, String value) {
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = nmsItem.getTag();
		
        if(tag == null)
            tag = new NBTTagCompound();
        
        tag.setString(key, value);
        nmsItem.setTag(tag);
        
        return CraftItemStack.asBukkitCopy(nmsItem);
	}
	
	public static String loadItemData(ItemStack item, String key) {
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = nmsItem.getTag();
		
        if(tag == null)
            return null;
        
        String result = tag.getString(key);
        
        return (result == null || result.isEmpty()) ? null : result;
	}
	
	public static String[] loadAllItemData(ItemStack item, String[] keys) {
		String[] values = new String[keys.length];
		
		for(int i = 0; i < keys.length; i++) {
			values[i] = loadItemData(item, keys[i]);
		}
		
		return values;
	}
	
	public static String[] loadAllOrNoneItemData(ItemStack item, String[] keys) {
		String[] values = new String[keys.length];
		
		for(int i = 0; i < keys.length; i++) {
			values[i] = loadItemData(item, keys[i]);
			
			if(values[i] == null) return null;
		}
		
		return values;
	}
}
