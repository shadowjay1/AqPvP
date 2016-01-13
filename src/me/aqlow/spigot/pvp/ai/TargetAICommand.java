package me.aqlow.spigot.pvp.ai;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import me.aqlow.spigot.pvp.AqPvP;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;

public class TargetAICommand implements AICommand {
	private EntityLiving target;
	
	public TargetAICommand(EntityLiving target) {
		this.target = target;
	}
	
	@Override
	public void run(final EntityInsentient entity, int commandTicks) {
		// some issue with modifying goal target to null here or something
		Bukkit.getScheduler().scheduleSyncDelayedTask(AqPvP.instance, new Runnable() {
			public void run() {
				entity.setGoalTarget(target, TargetReason.CUSTOM, false);
			}
		});
	}
}
