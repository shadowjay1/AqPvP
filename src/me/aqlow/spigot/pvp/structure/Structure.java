package me.aqlow.spigot.pvp.structure;

import org.bukkit.block.Block;

public interface Structure {
	public boolean isStructure(Block block);
	public boolean activateStructure(Block block);
	public void deactivateStructures();
	public void onTick();
}
