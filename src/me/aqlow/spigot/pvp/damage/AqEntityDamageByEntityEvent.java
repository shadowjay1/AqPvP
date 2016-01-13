package me.aqlow.spigot.pvp.damage;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AqEntityDamageByEntityEvent extends EntityDamageByEntityEvent {
	@SuppressWarnings("deprecation")
	public AqEntityDamageByEntityEvent(Entity damager, Entity damagee, DamageCause cause, double damage) {
		super(damager, damagee, cause, damage);
	}
}
