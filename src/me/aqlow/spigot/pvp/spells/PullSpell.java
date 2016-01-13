package me.aqlow.spigot.pvp.spells;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.spells.SpellProjectile.SpellProjectileHandler;
import me.aqlow.spigot.pvp.util.PlayerMap;
import me.aqlow.spigot.pvp.util.PseudoEntityWrapper;

public class PullSpell implements Castable, Targetable {
	private PlayerMap<Entity> targets = new PlayerMap<Entity>();
	
	public PullSpell() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			@Override
			public void run() {
				ArrayList<Player> toRemove = new ArrayList<Player>();
				for(Player p : targets.keySet()) {
					Entity target = targets.get(p);
					if(!p.getWorld().equals(target.getWorld())) {
						toRemove.add(p);
						continue;
					}
					if(p.getLocation().toVector().distanceSquared(target.getLocation().toVector()) < 1) {
						toRemove.add(p);
						continue;
					}
					Vector v = p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1);
					if(target instanceof Player) {
						target.teleport(target.getLocation().add(v));
					}
					else {
						net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) target).getHandle();
						boolean prevNoClip = nms.noclip;
						nms.noclip = true;
						nms.move(v.getX(), v.getY(), v.getZ());
						nms.noclip = prevNoClip;
						target.setVelocity(new Vector(0, 0, 0));
					}
				}
				targets.removeAll(toRemove);
			}
		}, 0, 1);
	}
	
	@Override
	public String getName() {
		return "Pull";
	}
	
	@Override
	public int getCost() {
		return 0;
	}

	@Override
	public boolean cast(final Player p) {
		if(targets.contains(p)) {
			targets.remove(p);
			return true;
		}
		
		SpellProjectile proj = new SpellProjectile(new PseudoEntityWrapper(p.getEyeLocation(), p.getLocation().getDirection().multiply(1.5), 30), 0.5);
		proj.setHandler(new SpellProjectileHandler() {
			@Override
			public boolean onHit(SpellProjectile proj, Entity e) {
				if(e.equals(p) || e.isDead()) return false;
				proj.getEntity().remove();
				targets.put(p, e);
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
				proj.getEntity().getLocation().getWorld().spigot().playEffect(loc, Effect.TILE_DUST, Material.OBSIDIAN.getId(), 0, 0.0F, 0.0F, 0.0F, 0.03F, 5, 50);
			}
		});
		proj.activate();
		
		return true;
	}
	
	@Override
	public boolean cast(Player p, Entity target) {
		return cast(p);
	}
	
	public long getCooldown() {
		return 200;
	}
}
