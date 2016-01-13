package me.aqlow.spigot.pvp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.aqlow.spigot.pvp.ai.AIManager;
import me.aqlow.spigot.pvp.damage.AqEntityDamageByEntityEvent;
import me.aqlow.spigot.pvp.spells.DrainSpell;
import me.aqlow.spigot.pvp.spells.LaunchSpell;
import me.aqlow.spigot.pvp.spells.RuptureSpell;
import me.aqlow.spigot.pvp.spells.SmiteSpell;
import me.aqlow.spigot.pvp.spells.SnareSpell;
import me.aqlow.spigot.pvp.spells.SpellProjectile;
import me.aqlow.spigot.pvp.structure.StructureManager;
import me.aqlow.spigot.pvp.util.ItemBlockerManager;
import net.md_5.bungee.api.ChatColor;

public class AqPvP extends JavaPlugin {
	public static AqPvP instance;
	
	public static int ATTACK_FREQUENCY = 1;
	public static MeleeMode MELEE_MODE = MeleeMode.TIME_SCALE;

	public enum MeleeMode {
		NEAR_TARGET, TIME_SCALE
	}

	private AttackManager attackManager = new AttackManager();
	
	public AqPvP() {
		instance = this;
	}
	
	public void onEnable() {
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		AqPvP.load(this.getConfig());
		
		// Register some event listeners
		this.getServer().getPluginManager().registerEvents(ItemBlockerManager.instance, this);
		this.getServer().getPluginManager().registerEvents(new LauncherManager(), this);
		this.getServer().getPluginManager().registerEvents(MagicManager.instance, this);
		this.getServer().getPluginManager().registerEvents(new CreatureManager(), this);
		this.getServer().getPluginManager().registerEvents(new AIManager(), this);
		this.getServer().getPluginManager().registerEvents(RuneManager.instance, this);
		SpellProjectile.registerListener();
		StructureManager.registerListener();
		
		// Near target mode: punching an entity causes the player to "target" the entity and will do damage
		//		over time if the player is near the target
		if(MELEE_MODE == MeleeMode.NEAR_TARGET) {
			this.getServer().getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.HIGHEST)
				public void onPlayerAttack(EntityDamageByEntityEvent event) {
					if(event.getCause() != DamageCause.ENTITY_ATTACK || event instanceof AqEntityDamageByEntityEvent) return;
					
					Entity damager = event.getDamager();

					if(damager instanceof Player && event.getEntity() instanceof LivingEntity) {
						if(!event.isCancelled()) {
							Player player = (Player) damager;
							event.setCancelled(true);

							attackManager.setTarget(player.getName(), (LivingEntity) event.getEntity());
						}
					}
				}

				@EventHandler
				public void onPlayerSneak(PlayerToggleSneakEvent event) {
					if(event.isSneaking()) {
						attackManager.removeTarget(event.getPlayer().getName());
					}
				}

				@EventHandler
				public void onPlayerSwitchItems(PlayerItemHeldEvent event) {
					attackManager.removeTarget(event.getPlayer().getName());
				}
			}, this);

			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					attackManager.attackAll();
				}
			}, 0, ATTACK_FREQUENCY);
		}
		// Time scale mode: attacks do damage based on the time since the last hit
		else if (MELEE_MODE == MeleeMode.TIME_SCALE) {
			this.getServer().getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.HIGHEST)
				public void onPlayerAttack(EntityDamageByEntityEvent event) {
					if(event.getCause() != DamageCause.ENTITY_ATTACK || event instanceof AqEntityDamageByEntityEvent) return;
					
					Entity damager = event.getDamager();
					
					if(damager instanceof Player && event.getEntity() instanceof LivingEntity) {
						if(!event.isCancelled()) {
							Player player = (Player) damager;
							event.setCancelled(true);

							attackManager.performAttack(player, (LivingEntity) event.getEntity());
							attackManager.attack(player.getName());
						}
					}
				}
			}, this);
		}
	}
	
	public void onDisable() {
		StructureManager.onDisable();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		if(!sender.hasPermission("aqpvp.config")) {
			return false;
		}
		
		if(command.getName().equals("aqpvp")) {
			if(args.length >= 2) {
				String path = args[0];
				String value = "";
				for(int i = 1; i < args.length; i++) {
					value += args[i];
					if(i < args.length - 1) {
						value += " ";
					}
				}
				if(this.getConfig().isDouble(path)) {
					this.getConfig().set(path, Double.parseDouble(value));
				}
				else if(this.getConfig().isInt(path)) {
					this.getConfig().set(path, Integer.parseInt(value));
				}
				else if(this.getConfig().isString(path)) {
					this.getConfig().set(path, value);
				}
				else {
					sender.sendMessage(ChatColor.RED + "Unknown config key");
					
					return false;
				}
				
				this.saveConfig();
				load(this.getConfig());
				
				sender.sendMessage(ChatColor.GREEN + "Successfully updated configuration");
			}
			else {
				sender.sendMessage(ChatColor.RED + "Usage: aqpvp <key> <value>");
			}
		}
		
		return true;
	}
	
	public static void load(FileConfiguration config) {
		AqPvP.ATTACK_FREQUENCY = config.getInt("fighter.attack-runnable-freq", 1);
		AqPvP.MELEE_MODE = MeleeMode.valueOf(config.getString("fighter.mode", "TIME_SCALE"));
		
		AttackManager.load(config);
		CustomWeapon.load(config);
		MagicManager.load(config);
		LauncherManager.load(config);
		RuneManager.load(config);
		SpellProjectile.load(config);
		
		DrainSpell.load(config);
		LaunchSpell.load(config);
		RuptureSpell.load(config);
		SmiteSpell.load(config);
		SnareSpell.load(config);
	}
}
