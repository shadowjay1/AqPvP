package me.aqlow.spigot.pvp.spells;

import org.bukkit.entity.Player;

public interface Castable extends Spell {
	public boolean cast(Player p);
}
