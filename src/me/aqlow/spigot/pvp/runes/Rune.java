package me.aqlow.spigot.pvp.runes;

public enum Rune {
	// Magic protection: reduces magic damage by X%
	// Quick launcher: reduces launcher cooldown by X%
	// Hunger regeneration: restores 1 hunger every X ticks
	// Tenacity: reduces Launch spell/Knockback enchant effect by X%, reduces Snare spell duration by X%
	// Ambush: Gives stealth when out-of-combat and still for X seconds
	
	MAGIC_PROTECTION("Magic protection"), QUICK_LAUNCHER("Quick launcher"), POWER_LAUNCHER("Power launcher"), HUNGER_REGEN("Hunger regeneration"),
	TENACITY("Tenacity"), AMBUSH("Ambush"), RAGE("Rage");
	
	private String id;
	
	private Rune(String id) {
		this.id = id;
	}
	
	public static Rune getById(String id) {
		for(Rune rune : Rune.values()) {
			if(rune.id.equals(id)) {
				return rune;
			}
		}
		
		return null;
	}
}
