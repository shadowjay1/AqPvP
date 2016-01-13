package me.aqlow.spigot.pvp.ai;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import me.aqlow.spigot.pvp.AqPvP;
import net.minecraft.server.v1_8_R3.EntityInsentient;

public class MoveAICommand implements AICommand {
	private Location loc;
	
	public MoveAICommand(Location loc) {
		this.loc = loc;
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
		
		if(entity.getBukkitEntity().getLocation().distanceSquared(loc) > 1 && commandTicks % 10 == 0) {
			entity.getNavigation().a(loc.getX(), loc.getY(), loc.getZ(), 1);
		}
	}
}
