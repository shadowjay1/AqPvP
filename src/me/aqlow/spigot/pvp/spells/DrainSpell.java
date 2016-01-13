package me.aqlow.spigot.pvp.spells;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.MagicManager;
import me.aqlow.spigot.pvp.damage.DamageManager;
import me.aqlow.spigot.pvp.damage.DamageType;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class DrainSpell implements Targetable {
	public static double DRAIN_RANGE = 7;
	public static int DRAIN_LENGTH = 10;
	public static int COST = 0;
	public static int COOLDOWN = 0;
	public static String DAMAGE_EQUATION = "";
	
	private static Expression damageEquation = null;
	private ArrayList<Drain> drains = new ArrayList<Drain>();
	
	public DrainSpell() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			private int tick = 0;
			
			public void run() {
				tick++;
				Iterator<Drain> iterator = drains.iterator();
				
				while(iterator.hasNext()) {
					Drain drain = iterator.next();
					
					if(tick % 10 == 0) {
						if(drain.target.getLocation().distance(drain.player.getLocation()) > DRAIN_RANGE || drain.player.isDead() || drain.target.isDead() || drain.count >= DRAIN_LENGTH) {
							iterator.remove();
							continue;
						}
						damageEquation.setVariable("ab", MagicManager.getScaling(drain.player));
						double healing = DamageManager.instance.doDamage(drain.player, drain.target, DamageType.MAGIC, damageEquation.evaluate());
						drain.player.setHealth(Math.min(drain.player.getHealth() + healing, 20));
						drain.count++;
					}
					
					for(int i = 0; i < (10 + (10 * MagicManager.getScaling(drain.player))); i++) {
						double percent = Math.random();
						Location location = drain.player.getEyeLocation().multiply(percent).add(drain.target.getEyeLocation().multiply(1 - percent));
						drain.player.getWorld().spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, -1.0F, 1.0F, 0.0F, 1, 0, 50);
					}
				}
			}
		}, 1, 1);
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN = config.getInt("mage.drain-spell.cooldown", 0);
		COST = config.getInt("mage.drain-spell.cost", 0);
		DAMAGE_EQUATION = config.getString("mage.drain-spell.damage-expr", "0");
		
		damageEquation = new ExpressionBuilder(DAMAGE_EQUATION)
			.variable("ab")
			.build();
	}
	
	@Override
	public String getName() {
		return "Drain";
	}

	@Override
	public int getCost() {
		return COST;
	}

	@Override
	public boolean cast(final Player p, final Entity target) {
		if(target instanceof LivingEntity) {
			Iterator<Drain> iterator = drains.iterator();
			while(iterator.hasNext()) {
				Drain drain = iterator.next();
				
				if(drain.player.getName().equals(p.getName())) {
					iterator.remove();
				}
			}
			
			drains.add(new Drain(p, (LivingEntity) target));
			((LivingEntity) target).addPotionEffect(PotionEffectType.SLOW.createEffect(100, 3));
			
			return true;
		}
		
		return false;
	}
	
	public long getCooldown() {
		return COOLDOWN;
	}
	
	private class Drain {
		private Player player;
		private LivingEntity target;
		private int count = 0;
		
		private Drain(Player p, LivingEntity target) {
			this.player = p;
			this.target = target;
		}
	}
}
