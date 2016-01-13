package me.aqlow.spigot.pvp.ai;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import me.aqlow.spigot.pvp.AqPvP;
import net.minecraft.server.v1_8_R3.EntityInsentient;

public class FollowAICommand implements AICommand {
	private Entity target;
	
	public FollowAICommand(Entity target) {
		this.target = target;
	}

	@Override
	public void run(final EntityInsentient entity, int commandTicks) {
		if(entity.getGoalTarget() != null) {
			// some issue with modifying goal target to null here or something
			Bukkit.getScheduler().scheduleSyncDelayedTask(AqPvP.instance, new Runnable() {
				public void run() {
					entity.setGoalTarget(null, TargetReason.CUSTOM, false);
				}
			});
		}
		Location loc = target.getLocation();
		if(commandTicks % 10 == 0)
			entity.getNavigation().a(loc.getX(), loc.getY(), loc.getZ(), 1);
	}
}
