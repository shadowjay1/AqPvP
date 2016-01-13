package me.aqlow.spigot.pvp.spells;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DebugSpell implements Targetable, Castable {
	// this spell is just for testing random things that require an entity and/or player
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Debug";
	}

	@Override
	public int getCost() {
		return 0;
	}

	@Override
	public long getCooldown() {
		return 100;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		return true;
	}

	@Override
	public boolean cast(Player p) {
		return true;
	}
}
