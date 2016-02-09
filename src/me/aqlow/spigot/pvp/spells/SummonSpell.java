package me.aqlow.spigot.pvp.spells;

import java.util.UUID;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.server.PluginDisableEvent;

import me.aqlow.spigot.pvp.AqPvP;
import me.aqlow.spigot.pvp.ai.AIUtil;
import me.aqlow.spigot.pvp.events.MageLoseFocusEvent;
import me.aqlow.spigot.pvp.util.EntityMap;
import net.minecraft.server.v1_8_R3.EntityInsentient;

public class SummonSpell implements Castable, Targetable, Listener {
	private final Class<? extends Entity> type;
	private final String name;
	private final int cost;
	private final int cooldown;
	
	private static EntityMap<Entity, UUID> map = new EntityMap<Entity, UUID>();
	
	public SummonSpell(Class<? extends Entity> type, int cost, int cooldown) {
		this.type = type;
		this.name = "Summon: " + type.getSimpleName();
		this.cost = cost;
		this.cooldown = cooldown;
	}
	
	public static EntityMap<Entity, UUID> getMap() {
		return map;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getCost() {
		return cost;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public boolean cast(Player p, Entity target) {
		return cast(p);
	}

	@Override
	public boolean cast(Player p) {
		Entity e = p.getWorld().spawn(p.getLocation(), type);
		CraftEntity ce = (CraftEntity) e;
		if(e instanceof LivingEntity) {
			((LivingEntity) e).setMaxHealth(0.1);
		}
		if(ce.getHandle() instanceof EntityInsentient) {
			EntityInsentient nms = (EntityInsentient) ce.getHandle();
			AIUtil.clearGoalSelector(nms);
			AIUtil.clearTargetSelector(nms);
		}
		map.put(e, p.getUniqueId());
		return true;
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onMageLoseFocus(MageLoseFocusEvent event) {
		for(Entity e : map.reverseGet(event.getPlayer().getUniqueId())) {
			map.remove(e);
			e.remove();
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if(map.containsKey(event.getEntity())) {
			event.setDroppedExp(0);
			event.getDrops().clear();
			map.remove(event.getEntity());
		}
	}
	
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if(event.getPlugin().equals(AqPvP.instance)) {
			for(Entity e : map.getAll()) {
				e.remove();
			}
			map.clear();
		}
	}
}
