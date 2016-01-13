package me.aqlow.spigot.pvp.ai;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import me.aqlow.spigot.pvp.AqPvP;

public class AIManager implements Listener {
	public static AIManager instance;
	private HashMap<String, AICommand> commands = new HashMap<String, AICommand>();
	
	public AIManager() {
		instance = this;
	}
	
	public void setCommand(String player, AICommand command) {
		this.commands.put(player, command);
	}
	
	public AICommand getCommand(String player) {
		return commands.get(player);
	}
	
	// TODO - make sure you can't name the snowball "Command", ensure lore "Command" works
	@EventHandler
	public void onSnowballThrow(ProjectileLaunchEvent event) {
		if(event.getEntity() instanceof Snowball) {
			ProjectileSource shooter = event.getEntity().getShooter();
			
			if(shooter != null && shooter instanceof Player) {
				ItemStack item = ((Player) shooter).getItemInHand();
				ItemMeta meta = item.getItemMeta();
				if(meta == null) return;
				if(!meta.hasLore()) return;
				if(meta.getLore().size() < 1) return;
				if(!meta.getLore().get(0).equals("Command")) return;
				
				event.getEntity().setCustomName("Command");
			}
		}
	}
	
	@EventHandler
	public void onSnowballHit(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Snowball) {
			final Snowball snowball = (Snowball) event.getDamager();
			
			if(snowball.getCustomName() != null && snowball.getCustomName().equals("Command") && event.getEntity() instanceof LivingEntity) {
				final LivingEntity target = (LivingEntity) event.getEntity();

				if(snowball.getShooter() != null && snowball.getShooter() instanceof Player) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(AqPvP.instance, new Runnable() {
						public void run() {
							setCommand(((Player) snowball.getShooter()).getName(), new TargetAICommand(((CraftLivingEntity) target).getHandle()));
						}
					});
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onSnowballLand(ProjectileHitEvent event) {
		if(event.getEntity() instanceof Snowball) {
			Snowball snowball = (Snowball) event.getEntity();

			if(snowball.getCustomName() != null && snowball.getCustomName().equals("Command") && snowball.getShooter() != null && snowball.getShooter() instanceof Player) {
				setCommand(((Player) snowball.getShooter()).getName(), new MoveAICommand(snowball.getLocation()));
			}
		}
	}
	
	@EventHandler
	public void onSnowballDrop(PlayerDropItemEvent event) {
		ItemStack item = event.getItemDrop().getItemStack();
		ItemMeta meta = item.getItemMeta();
		if(meta == null) return;
		if(!meta.hasLore()) return;
		if(meta.getLore().size() < 1) return;
		if(!meta.getLore().get(0).equals("Command")) return;
		
		if(item.getType() == Material.SNOW_BALL) {
			if(item.getAmount() > 1) {
				item.setAmount(item.getAmount() - 1);
				event.getItemDrop().setItemStack(item);
			}
			else {
				event.getItemDrop().remove();
			}
			
			setCommand(event.getPlayer().getName(), new FollowAICommand(event.getPlayer()));
		}
	}
}
