package me.aqlow.spigot.pvp.ai;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;

import me.aqlow.spigot.pvp.CreatureManager;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.PathfinderGoal;

public class AIPathfinderGoal extends PathfinderGoal {
	private String owner = null;
	private EntityInsentient entity;
	private AICommand lastCommand;
	private int commandTicks;
	
	public AIPathfinderGoal(EntityInsentient entity) {
		this.entity = entity;
		
		UUID uuid = CreatureManager.instance.getCreature((LivingEntity) entity.getBukkitEntity());
		if(uuid != null) {
			this.owner = CreatureManager.instance.getOwner(uuid);
		}
	}
	
	public AIPathfinderGoal setOwner(String owner) {
		this.owner = owner;
		
		return this;
	}
	
	// should execute
	@Override
	public boolean a() {
		if(owner == null) return false;
		
		return AIManager.instance.getCommand(owner) != null;
	}
	
	// task update
	@Override
	public void e() {
		if(owner == null) return;
		
		AICommand command = AIManager.instance.getCommand(owner);
		if(command != null) {
			if(lastCommand == null || !lastCommand.equals(command)) {
				commandTicks = 0;
				lastCommand = command;
			}
			command.run(this.entity, commandTicks);
			commandTicks++;
		}
	}
}
