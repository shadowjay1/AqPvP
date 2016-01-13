package me.aqlow.spigot.pvp.spells;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import me.aqlow.spigot.pvp.MagicManager;

public class WitherSpell implements Targetable {
	@Override
	public String getName() {
		return "Wither";
	}

	@Override
	public int getCost() {
		return 15;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		if(target instanceof LivingEntity) {
			((LivingEntity) target).addPotionEffect(PotionEffectType.WITHER.createEffect((int) (200 + (300 * MagicManager.getScaling(p))), 4));
			return true;
		}
		
		return false;
	}
	
	public long getCooldown() {
		return 10000;
	}
}
