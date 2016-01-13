package me.aqlow.spigot.pvp.spells;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface Targetable extends Spell {
	public boolean cast(Player p, Entity target);
}
