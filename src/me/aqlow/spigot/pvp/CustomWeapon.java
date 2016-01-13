package me.aqlow.spigot.pvp;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.aqlow.spigot.pvp.runes.Rune;

// TODO: make this nicer in case we decide to add more custom weapons
// (charge time should be per-weapon)
public enum CustomWeapon {
	LAUNCHER(3000);
	
	private int chargeTime;
	
	private CustomWeapon(int chargeTime) {
		this.chargeTime = chargeTime;
	}
	
	public int getChargeTime(Player p) {
		if(RuneManager.instance.hasRune(p, Rune.QUICK_LAUNCHER)) {
			return (int) (chargeTime * (1 - RuneManager.LAUNCHER_COOLDOWN_REDUCTION));
		}
		
		return chargeTime;
	}
	
	public static CustomWeapon getType(ItemStack item) {
		if(item == null) {
			return null;
		}
		
		if(item.getType() == Material.GOLD_HOE) {
			return LAUNCHER;
		}
		
		return null;
	}
	
	public static void load(FileConfiguration config) {
		LAUNCHER.chargeTime = config.getInt("launcher.launcher-charge-ms");
	}
}
