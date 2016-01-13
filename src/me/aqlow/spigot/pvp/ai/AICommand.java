package me.aqlow.spigot.pvp.ai;

import net.minecraft.server.v1_8_R3.EntityInsentient;

public interface AICommand {
	public void run(EntityInsentient entity, int commandTicks);
}
