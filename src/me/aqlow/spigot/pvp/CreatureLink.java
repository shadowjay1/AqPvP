package me.aqlow.spigot.pvp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.aqlow.spigot.pvp.util.AqUtil;

public final class CreatureLink {
	private final String player;
	private final CreatureInfo restoreInfo;
	
	public CreatureLink(String player, CreatureInfo info) {
		this.player = player;
		this.restoreInfo = info.kill();
	}
	
	public String getPlayer() {
		return player;
	}
	
	public CreatureInfo getLastInfo() {
		return restoreInfo;
	}
	
	public List<String> toLore() {
		List<String> lore = new ArrayList<String>(restoreInfo.toLore());
		lore.add(0, AqUtil.encodeStrings(new String[] {"link"}, "Active"));
		
		return lore;
	}
	
	public ItemStack toItem() {
		ItemStack newItem = this.getLastInfo().toItem();
		newItem.setDurability((short) 0);
		ItemMeta meta = newItem.getItemMeta();
		meta.setLore(this.toLore());
		newItem.setItemMeta(meta);
		newItem = AqUtil.saveItemData(newItem, "aqpvp:creature:linked", "true");
		
		return newItem;
	}
	
	public static CreatureLink fromMeta(String player, ItemStack item) {
		if(AqUtil.loadItemData(item, "aqpvp:creature:linked") != null) {
			CreatureInfo info = CreatureInfo.fromMeta(item, true);
			if(info != null) {
				return new CreatureLink(player, info);
			}
		}
		
		return null;
	}
}
