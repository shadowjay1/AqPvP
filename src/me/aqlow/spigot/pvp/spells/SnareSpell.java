package me.aqlow.spigot.pvp.spells;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.MagicManager;
import me.aqlow.spigot.pvp.RuneManager;
import me.aqlow.spigot.pvp.runes.Rune;
import me.aqlow.spigot.pvp.spells.SpellProjectile.SpellProjectileHandler;
import me.aqlow.spigot.pvp.util.PseudoEntityWrapper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class SnareSpell implements Castable, Targetable {
	private ArrayList<WeakReference<Arrow>> arrows = new ArrayList<WeakReference<Arrow>>();
	
	public static int COST = 0;
	public static int COOLDOWN = 0;
	public static int COOLDOWN_INCREASE = 0;
	public static String DURATION_EQUATION = "";
	
	private static Expression durationEquation = null;
	
	public SnareSpell() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				Iterator<WeakReference<Arrow>> iterator2 = arrows.iterator();
				
				while(iterator2.hasNext()) {
					WeakReference<Arrow> ref = iterator2.next();
					Arrow arrow = ref.get();
					
					if(arrow == null || arrow.isDead()) {
						iterator2.remove();
						continue;
					}
					
					durationEquation.setVariable("ab", MagicManager.getScaling((Player) arrow.getShooter()));
					
					double duration = durationEquation.evaluate();
					
					if(arrow.getPassenger() != null && arrow.getPassenger() instanceof Player && RuneManager.instance.hasRune((Player) arrow.getPassenger(), Rune.TENACITY)) {
						duration *= (1 - RuneManager.TENACITY_REDUCTION);
					}
					
					if(arrow.getTicksLived() > duration) {
						arrow.remove();
						iterator2.remove();
					}
				}
			}
		}, 1, 1);
		
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener() {
			@Override
			public Plugin getPlugin() {
				return AqPvP.instance;
			}

			@Override
			public ListeningWhitelist getReceivingWhitelist() {
				return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.STEER_VEHICLE).build();
			}

			@Override
			public ListeningWhitelist getSendingWhitelist() {
				return ListeningWhitelist.EMPTY_WHITELIST;
			}

			@Override
			public void onPacketReceiving(PacketEvent event) {
				if(event.getPacket().getBooleans().read(1)) {
					Entity vehicle = event.getPlayer().getVehicle();
					if(vehicle == null) return;
					
					Iterator<WeakReference<Arrow>> iterator = arrows.iterator();
					
					while(iterator.hasNext()) {
						WeakReference<Arrow> ref = iterator.next();
						Arrow arrow = ref.get();
						
						if(arrow == null || arrow.isDead()) {
							iterator.remove();
							continue;
						}
						
						if(arrow.getUniqueId().equals(vehicle.getUniqueId())) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}

			@Override
			public void onPacketSending(PacketEvent event) {}
			
		});
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if(event.getCause() == TeleportCause.ENDER_PEARL) {
			Entity vehicle = event.getPlayer().getVehicle();
			
			if(vehicle != null && vehicle instanceof Arrow) {
				Iterator<WeakReference<Arrow>> iterator = arrows.iterator();
				
				while(iterator.hasNext()) {
					WeakReference<Arrow> ref = iterator.next();
					Arrow arrow = ref.get();
					
					if(arrow == null || arrow.isDead()) {
						iterator.remove();
						continue;
					}
					
					if(arrow.getUniqueId().equals(vehicle.getUniqueId())) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Arrow) {
			Iterator<WeakReference<Arrow>> iterator = arrows.iterator();

			while(iterator.hasNext()) {
				WeakReference<Arrow> ref = iterator.next();
				Arrow arrow = ref.get();

				if(arrow == null || arrow.isDead()) {
					iterator.remove();
					continue;
				}

				if(arrow.getUniqueId().equals(event.getDamager().getUniqueId())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@Override
	public String getName() {
		return "Snare";
	}

	@Override
	public int getCost() {
		return COST;
	}

	@Override
	public long getCooldown() {
		return COOLDOWN;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		return cast(p);
	}

	@Override
	public boolean cast(final Player p) {
		SpellProjectile proj = new SpellProjectile(new PseudoEntityWrapper(p.getEyeLocation(), p.getLocation().getDirection().multiply(1.5), 30), 0.5);
		proj.setHandler(new SpellProjectileHandler() {
			@Override
			public boolean onHit(SpellProjectile proj, Entity e) {
				if(e.equals(p) || !(e instanceof LivingEntity) || e.isDead()) return false;
				Location loc = e.getLocation();
				if(e.getVehicle() != null && e.getVehicle() instanceof Arrow) {
					loc = e.getVehicle().getLocation();
				}
				Arrow arrow = e.getWorld().spawn(loc, Arrow.class);
				arrow.setPassenger(e);
				arrow.setShooter(proj.getShooter());
				arrows.add(new WeakReference<Arrow>(arrow));
				
				MagicManager.instance.increaseCooldown(((Player) proj.getShooter()).getName(), SnareSpell.this, COOLDOWN_INCREASE);
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
				proj.getEntity().getLocation().getWorld().spigot().playEffect(loc, Effect.TILE_DUST, Material.WEB.getId(), 0, 0.0F, 0.0F, 0.0F, 0.03F, 5, 50);
			}
		});
		proj.setShooter(p);
		proj.activate();
		
		return true;
	}
	
	public static void load(FileConfiguration config) {
		COOLDOWN = config.getInt("mage.snare-spell.cooldown", 0);
		COST = config.getInt("mage.snare-spell.cost", 0);
		DURATION_EQUATION = config.getString("mage.snare-spell.duration-expr", "0");
		COOLDOWN_INCREASE = config.getInt("mage.snare-spell.cooldown-increase", 0);
		
		durationEquation = new ExpressionBuilder(DURATION_EQUATION)
			.variable("ab")
			.build();
	}
}
