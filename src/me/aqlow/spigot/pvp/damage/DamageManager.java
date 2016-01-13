package me.aqlow.spigot.pvp.damage;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import me.aqlow.spigot.pvp.RuneManager;
import me.aqlow.spigot.pvp.runes.Rune;

public class DamageManager {
	public static final DamageManager instance = new DamageManager();
	
	public double doDamage(LivingEntity damager, LivingEntity entity, DamageType type, double damage) {
		switch(type) {
			case PHYSICAL:
				if(tryEvent(damager, entity, damage)) {
					entity.damage(damage);
					return damage;
				}
				else {
					return 0;
				}
			case MAGIC:
				if(entity instanceof Player) {
					if(RuneManager.instance.hasRune((Player) entity, Rune.MAGIC_PROTECTION)) {
						damage *= (1 - RuneManager.MAGIC_DAMAGE_REDUCTION);
					}
				}
				if(tryEvent(damager, entity, damage)) {
					entity.damage(damage);
					return damage;
				}
				else {
					return 0;
				}
			default:
				return 0;
		}
	}
	
	private boolean tryEvent(LivingEntity damager, LivingEntity entity, double damage) {
		AqEntityDamageByEntityEvent event = new AqEntityDamageByEntityEvent(damager, entity, DamageCause.CUSTOM, damage);
		Bukkit.getPluginManager().callEvent(event);
		
		if(!event.isCancelled()) {
			entity.setLastDamage(damage);
			entity.setLastDamageCause(event);
			return true;
		}
		else {
			return false;
		}
	}
}
