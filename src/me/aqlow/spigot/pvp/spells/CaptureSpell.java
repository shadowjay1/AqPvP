package me.aqlow.spigot.pvp.spells;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.aqlow.spigot.pvp.CreatureInfo;
import me.aqlow.spigot.pvp.CreatureManager;

public class CaptureSpell implements Targetable {
	private ItemStack[] cost;
	
	public CaptureSpell() {
		this.cost = new ItemStack[1];
		this.cost[0] = new ItemStack(Material.ENDER_PEARL);
		ItemMeta meta = this.cost[0].getItemMeta();
		meta.setLore(Arrays.asList("Spherical capturing device"));
		this.cost[0].setItemMeta(meta);
	}
	
	@Override
	public String getName() {
		return "Capture";
	}

	@Override
	public int getCost() {
		return 0;
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		if(target instanceof LivingEntity && !(target instanceof Player)) {
			if(CreatureManager.instance.getCreature((LivingEntity) target) == null) {
				if(SpellUtil.removeItems(p.getInventory(), cost)) {
					CreatureInfo info = CreatureInfo.fromEntity((LivingEntity) target);
					p.getWorld().dropItem(target.getLocation(), info.toItem());
					
					target.remove();
					return true;
				}
			}
		}
		
		return false;
	}
	
}
