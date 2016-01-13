package me.aqlow.spigot.pvp.structure;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import me.aqlow.spigot.pvp.AqPvP;

public class StructureManager implements Listener {
	private static StructureManager instance = new StructureManager();
	private ArrayList<Structure> structures = new ArrayList<Structure>();
	
	private static int lastTask = -1;
	
	public StructureManager() {
		structures.add(new TractorBeam());
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
			for(Structure structure : structures) {
				if(structure.isStructure(event.getClickedBlock())) {
					structure.activateStructure(event.getClickedBlock());
					return;
				}
			}
		}
	}
	
	public static void onDisable() {
		for(Structure structure : instance.structures) {
			structure.deactivateStructures();
		}
	}
	
	public static void registerListener() {
		Bukkit.getPluginManager().registerEvents(instance, AqPvP.instance);
		for(Structure structure : instance.structures) {
			if(structure instanceof Listener) {
				Bukkit.getPluginManager().registerEvents((Listener) structure, AqPvP.instance);
			}
		}
		
		if(lastTask > -1) Bukkit.getScheduler().cancelTask(lastTask);
		lastTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AqPvP.instance, new Runnable() {
			public void run() {
				for(Structure structure : instance.structures) {
					structure.onTick();
				}
			}
		}, 0, 1);
	}
}
