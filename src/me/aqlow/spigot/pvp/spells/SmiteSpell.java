package me.aqlow.spigot.pvp.spells;

import java.util.List;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import me.aqlow.spigot.pvp.MagicManager;
import me.aqlow.spigot.pvp.damage.DamageManager;
import me.aqlow.spigot.pvp.damage.DamageType;
import me.aqlow.spigot.pvp.spells.SpellProjectile.SpellProjectileHandler;
import me.aqlow.spigot.pvp.util.PseudoEntityWrapper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class SmiteSpell implements Targetable, Castable {
	public static int COST = 0;
	public static int COOLDOWN = 0;
	public static String DAMAGE_EQUATION = "";
	
	private static Expression damageEquation = null;
	
	public String getName() {
		return "Smite";
	}
	
	public int getCost() {
		return COST;
	}
	
	public boolean cast(Player p, Entity target) {
		smite(p, target.getLocation());
		return true;
	}
	
	public boolean cast(Player p) {
		List<Block> targetBlocks = p.getLastTwoTargetBlocks((Set<Material>) null, 200);
		if(targetBlocks.size() > 0) {
			smite(p, targetBlocks.get(0).getLocation());
			return true;
		}
		
		return false;
	}
	
	public void smite(final Player p, Location l) {
		SpellProjectile proj = new SpellProjectile(new PseudoEntityWrapper(p.getEyeLocation(), p.getLocation().getDirection().multiply(1.5), 30), 0.5);
		proj.setHandler(new SpellProjectileHandler() {
			@Override
			public boolean onHit(SpellProjectile proj, Entity e) {
				if(e.equals(p) || !(e instanceof LivingEntity) || e.isDead()) return false;
				damageEquation.setVariable("ab", MagicManager.getScaling(p));
				DamageManager.instance.doDamage(p, (LivingEntity) e, DamageType.MAGIC, damageEquation.evaluate());
				e.setFireTicks(10);
				e.getWorld().strikeLightningEffect(e.getLocation());
				proj.getEntity().remove();
				return true;
			}
			
			@SuppressWarnings("deprecation")
			@Override
			public void onTick(SpellProjectile proj) {
				double r = Math.random();
				Location loc;
				if(proj.getLastLocation() != null) {
					loc = proj.getEntity().getLocation().clone().multiply(r).add(proj.getLastLocation().multiply(1 - r));
				}
				else {
					loc = proj.getEntity().getLocation();
				}
				proj.getEntity().getLocation().getWorld().spigot().playEffect(loc, Effect.TILE_DUST, Material.LAVA.getId(), 0, 0.0F, 0.0F, 0.0F, 0.03F, 5, 50);
			}
		});
		proj.activate();
	}
	
	public long getCooldown() {
		return COOLDOWN;
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN = config.getInt("mage.smite-spell.cooldown", 0);
		COST = config.getInt("mage.smite-spell.cost", 0);
		DAMAGE_EQUATION = config.getString("mage.smite-spell.damage-expr", "0");
		
		damageEquation = new ExpressionBuilder(DAMAGE_EQUATION)
			.variable("ab")
			.build();
	}
}
