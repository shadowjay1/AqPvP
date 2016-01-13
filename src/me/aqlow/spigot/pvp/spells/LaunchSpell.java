package me.aqlow.spigot.pvp.spells;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import me.aqlow.spigot.pvp.MagicManager;
import me.aqlow.spigot.pvp.RuneManager;
import me.aqlow.spigot.pvp.runes.Rune;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class LaunchSpell implements Targetable {
	public static int COOLDOWN = 0;
	public static int COST = 0;
	public static String MAGNITUDE_EQUATION = "";
	
	private static Expression magnitudeEquation = null;
	
	@Override
	public String getName() {
		return "Launch";
	}

	@Override
	public int getCost() {
		return COST;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		if(target.isInsideVehicle()) {
			return cast(p, target.getVehicle());
		}
		
		magnitudeEquation.setVariable("ab", MagicManager.getScaling(p));
		double amplitude = magnitudeEquation.evaluate();
		if(target instanceof Player && RuneManager.instance.hasRune((Player) target, Rune.TENACITY)) {
			amplitude *= (1 - RuneManager.TENACITY_REDUCTION);
		}
		target.setVelocity(p.getLocation().getDirection().setY(0.5).normalize().multiply(amplitude));
		return true;
	}
	
	public long getCooldown() {
		return COOLDOWN;
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN = config.getInt("mage.launch-spell.cooldown", 0);
		COST = config.getInt("mage.launch-spell.cost", 0);
		MAGNITUDE_EQUATION = config.getString("mage.launch-spell.magnitude-expr", "0");
		
		magnitudeEquation = new ExpressionBuilder(MAGNITUDE_EQUATION).variable("ab").build();
	}
}
