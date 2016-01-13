package me.aqlow.spigot.pvp;

import static org.bukkit.entity.EntityType.CAVE_SPIDER;
import static org.bukkit.entity.EntityType.ENDERMITE;
import static org.bukkit.entity.EntityType.PIG_ZOMBIE;
import static org.bukkit.entity.EntityType.SILVERFISH;
import static org.bukkit.entity.EntityType.SKELETON;
import static org.bukkit.entity.EntityType.SPIDER;
import static org.bukkit.entity.EntityType.WITHER;
import static org.bukkit.entity.EntityType.ZOMBIE;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.aqlow.spigot.pvp.damage.DamageManager;
import me.aqlow.spigot.pvp.damage.DamageType;
import me.aqlow.spigot.pvp.runes.Rune;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class AttackManager {
	public static AttackManager instance = null;
	
	private static List<EntityType> UNDEAD_TYPES = Arrays.asList(
		SKELETON, ZOMBIE, WITHER, PIG_ZOMBIE
	);
	private static List<EntityType> ARTHROPOD_TYPES = Arrays.asList(
		SPIDER, CAVE_SPIDER, SILVERFISH, ENDERMITE
	);
	
	private HashMap<String, Long> cooldowns = new HashMap<String, Long>();
	private HashMap<String, WeakReference<LivingEntity>> targets = new HashMap<String, WeakReference<LivingEntity>>();
	private HashMap<String, Long> lastAttack = new HashMap<String, Long>();
	
	public static int ATTACK_COOLDOWN_MS = 500;
	public static double ATTACK_RANGE = 4;
	public static int TIME_SCALE_MS = 500;
	
	// Vanilla weapon/armor values
	public static HashMap<Material, Double> weapons = new HashMap<Material, Double>();
	public static HashMap<Material, Double> armor = new HashMap<Material, Double>();

	// base_dmg, sharp_level, other_level, strength_level, weakness_level, base_armor, prot_level
	public static String DAMAGE_EXPRESSION = "0.5 * (base_dmg + 1.25(sharp_level) + 2.5(other_level) - 0.5(weakness_level)) * (1 + (strength_level * 1.3)) * (1 - base_armor) * ((20 - prot_level) / 20)";
	public static String ARMOR_DMG_EXPRESSION = "0.1 * (base_dmg + 1.25(sharp_level) + 2.5(other_level) - 0.5(weakness_level)) * (1 + (strength_level * 1.3)) * ((60 + (40 / (unbr_level + 1))) / 100)";
	public static String SWORD_DMG_EXPRESSION = "1 / (unbr_level + 1)";
	public static String KNOCKBACK_EXPRESSION = "";
	
	private static Expression dmgExpression;
	private static Expression armorDmgExpression;
	private static Expression swordDmgExpression;
	private static Expression knockbackExpression;
	
	static {
		// vanilla damage values
		weapons.put(Material.DIAMOND_SWORD, 8.0);
		weapons.put(Material.IRON_SWORD, 7.0);
		weapons.put(Material.STONE_SWORD, 6.0);
		weapons.put(Material.GOLD_SWORD, 5.0);
		weapons.put(Material.WOOD_SWORD, 5.0);
		weapons.put(Material.DIAMOND_AXE, 7.0);
		weapons.put(Material.IRON_AXE, 6.0);
		weapons.put(Material.STONE_AXE, 5.0);
		weapons.put(Material.GOLD_AXE, 4.0);
		weapons.put(Material.WOOD_AXE, 4.0);
		weapons.put(Material.DIAMOND_PICKAXE, 6.0);
		weapons.put(Material.IRON_PICKAXE, 5.0);
		weapons.put(Material.STONE_PICKAXE, 4.0);
		weapons.put(Material.GOLD_PICKAXE, 3.0);
		weapons.put(Material.WOOD_PICKAXE, 3.0);
		weapons.put(Material.DIAMOND_SPADE, 5.0);
		weapons.put(Material.IRON_SPADE, 4.0);
		weapons.put(Material.STONE_SPADE, 3.0);
		weapons.put(Material.GOLD_SPADE, 2.0);
		weapons.put(Material.WOOD_SPADE, 2.0);

		// vanilla armor values
		armor.put(Material.DIAMOND_HELMET, 0.12);
		armor.put(Material.DIAMOND_CHESTPLATE, 0.32);
		armor.put(Material.DIAMOND_LEGGINGS, 0.24);
		armor.put(Material.DIAMOND_BOOTS, 0.12);
		armor.put(Material.IRON_HELMET, 0.08);
		armor.put(Material.IRON_CHESTPLATE, 0.24);
		armor.put(Material.IRON_LEGGINGS, 0.20);
		armor.put(Material.IRON_BOOTS, 0.08);
		armor.put(Material.CHAINMAIL_HELMET, 0.08);
		armor.put(Material.CHAINMAIL_CHESTPLATE, 0.20);
		armor.put(Material.CHAINMAIL_LEGGINGS, 0.16);
		armor.put(Material.CHAINMAIL_BOOTS, 0.04);
		armor.put(Material.GOLD_HELMET, 0.08);
		armor.put(Material.GOLD_CHESTPLATE, 0.20);
		armor.put(Material.GOLD_LEGGINGS, 0.12);
		armor.put(Material.GOLD_BOOTS, 0.04);
		armor.put(Material.LEATHER_HELMET, 0.04);
		armor.put(Material.LEATHER_CHESTPLATE, 0.12);
		armor.put(Material.LEATHER_LEGGINGS, 0.08);
		armor.put(Material.LEATHER_BOOTS, 0.04);
	}
	
	public AttackManager() {
		instance = this;
	}
	
	public static void load(FileConfiguration config) {
		ATTACK_COOLDOWN_MS = config.getInt("fighter.attack-cooldown-ms", 500);
		ATTACK_RANGE = config.getInt("fighter.attack-range", 4);
		TIME_SCALE_MS = config.getInt("fighter.time-scale-ms", 500);
		
		DAMAGE_EXPRESSION = config.getString("fighter.damage-expr", "0");
		ARMOR_DMG_EXPRESSION = config.getString("fighter.armor-dmg-expr", "0");
		SWORD_DMG_EXPRESSION = config.getString("fighter.sword-dmg-expr", "0");
		KNOCKBACK_EXPRESSION = config.getString("fighter.knockback-expr", "0");
		
		dmgExpression = new ExpressionBuilder(DAMAGE_EXPRESSION)
			.variable("base_dmg")
			.variable("sharp_level")
			.variable("strength_level")
			.variable("weakness_level")
			.variable("other_level")
			.variable("base_armor")
			.variable("prot_level")
			.build();
			
		armorDmgExpression = new ExpressionBuilder(ARMOR_DMG_EXPRESSION)
			.variable("base_dmg")
			.variable("sharp_level")
			.variable("strength_level")
			.variable("weakness_level")
			.variable("other_level")
			.variable("base_armor")
			.variable("prot_level")
			.variable("unbr_level")
			.build();
			
		swordDmgExpression = new ExpressionBuilder(SWORD_DMG_EXPRESSION)
			.variable("base_dmg")
			.variable("sharp_level")
			.variable("strength_level")
			.variable("weakness_level")
			.variable("other_level")
			.variable("base_armor")
			.variable("prot_level")
			.variable("unbr_level")
			.build();
		
		knockbackExpression = new ExpressionBuilder(KNOCKBACK_EXPRESSION)
			.variables("sprinting", "knockback_level")
			.build();
	}
	
	// (Near target mode) Attacks all targets
	public void attackAll() {
		for(String player : targets.keySet()) {
			WeakReference<LivingEntity> entityRef = targets.get(player);
			LivingEntity entity = entityRef.get();

			if(entity == null || entity.isDead()) {
				targets.remove(player);
				continue;
			}

			Player playerObj = Bukkit.getPlayer(player);

			if(playerObj == null || playerObj.isDead()) {
				targets.remove(player);
				continue;
			}

			if(playerObj.getLocation().distance(entity.getLocation()) <= ATTACK_RANGE) {
				if(attack(player)) {
					performAttack(playerObj, entity);
				}
			}
		}
	}
	
	// (Near target mode) Returns a player's target
	public LivingEntity getTarget(String player) {
		if(!targets.containsKey(player)) return null;

		WeakReference<LivingEntity> entityRef = targets.get(player);
		LivingEntity entity = entityRef.get();

		if(entity == null) targets.remove(player);

		return entity;
	}
	
	// (Near target mode) Sets a player's target
	public void setTarget(String player, LivingEntity entity) {
		targets.put(player, new WeakReference<LivingEntity>(entity));
	}
	
	// (Near target mode) Removes a player's target
	public void removeTarget(String player) {
		targets.remove(player);
	}
	
	// Calculates an attack's damage
	public void performAttack(Player player, LivingEntity entity) {
		double baseDmg;

		ItemStack itemInHand = player.getInventory().getItemInHand();
		
		// Base damage = vanilla damage based on weapon type
		if(itemInHand != null && weapons.containsKey(itemInHand.getType())) {
			baseDmg = weapons.get(itemInHand.getType());
		}
		else {
			baseDmg = 1;
		}

		double sharpLevel = 0;
		double strengthLevel = 0;
		double weaknessLevel = 0;
		double otherLevel = 0;
		
		for(PotionEffect effect : player.getActivePotionEffects()) {
			if(effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
				if(effect.getAmplifier() + 1 > strengthLevel) {
					strengthLevel = effect.getAmplifier() + 1;
				}
			}
			if(effect.getType().equals(PotionEffectType.WEAKNESS)) {
				if(effect.getAmplifier() + 1 > weaknessLevel) {
					weaknessLevel = effect.getAmplifier() + 1;
				}
			}
		}

		if(itemInHand != null) {
			sharpLevel = itemInHand.getEnchantmentLevel(Enchantment.DAMAGE_ALL);

			if(UNDEAD_TYPES.contains(entity.getType())) {
				otherLevel = itemInHand.getEnchantmentLevel(Enchantment.DAMAGE_UNDEAD);
			}
			else if(ARTHROPOD_TYPES.contains(entity.getType())) {
				otherLevel = itemInHand.getEnchantmentLevel(Enchantment.DAMAGE_ARTHROPODS);
			}
		}
		
		armorDmgExpression.setVariable("base_dmg", baseDmg);
		armorDmgExpression.setVariable("sharp_level", sharpLevel);
		armorDmgExpression.setVariable("strength_level", strengthLevel);
		armorDmgExpression.setVariable("weakness_level", weaknessLevel);
		armorDmgExpression.setVariable("other_level", otherLevel);
		
		// Time-scaling
		double timeFactor = 1;
		if(lastAttack.containsKey(player.getName())) {
			long timeSince = System.currentTimeMillis() - lastAttack.get(player.getName());
			if(timeSince < TIME_SCALE_MS) {
				timeFactor = (double) timeSince / (double) TIME_SCALE_MS;
			}
		}
		
		double baseArmor = 0;
		int protLevel = 0;

		if(entity instanceof HumanEntity) {
			HumanEntity targetHuman = (HumanEntity) entity;

			ItemStack helmet = targetHuman.getInventory().getHelmet();
			ItemStack chestplate = targetHuman.getInventory().getChestplate();
			ItemStack leggings = targetHuman.getInventory().getLeggings();
			ItemStack boots = targetHuman.getInventory().getBoots();

			if(helmet != null)  {
				double basePart = 0;
				if(armor.containsKey(helmet.getType())) {
					basePart = armor.get(helmet.getType());
				}
				double protPart = helmet.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
				double unbrPart = helmet.getEnchantmentLevel(Enchantment.DURABILITY);
				baseArmor += basePart;
				protLevel += protPart;
				armorDmgExpression.setVariable("base_armor", basePart);
				armorDmgExpression.setVariable("prot_level", protPart);
				armorDmgExpression.setVariable("unbr_level", unbrPart);
				targetHuman.getInventory().setHelmet(doItemDamage(helmet, armorDmgExpression.evaluate() * timeFactor));
			}
			if(chestplate != null) {
				double basePart = 0;
				if(armor.containsKey(chestplate.getType())) {
					basePart = armor.get(chestplate.getType());
				}
				double protPart = chestplate.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
				double unbrPart = chestplate.getEnchantmentLevel(Enchantment.DURABILITY);
				baseArmor += basePart;
				protLevel += protPart;
				armorDmgExpression.setVariable("base_armor", basePart);
				armorDmgExpression.setVariable("prot_level", protPart);
				armorDmgExpression.setVariable("unbr_level", unbrPart);
				targetHuman.getInventory().setChestplate(doItemDamage(chestplate, armorDmgExpression.evaluate() * timeFactor));
			}
			if(leggings != null) {
				double basePart = 0;
				if(armor.containsKey(leggings.getType())) {
					basePart = armor.get(leggings.getType());
				}
				double protPart = leggings.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
				double unbrPart = leggings.getEnchantmentLevel(Enchantment.DURABILITY);
				baseArmor += basePart;
				protLevel += protPart;
				armorDmgExpression.setVariable("base_armor", basePart);
				armorDmgExpression.setVariable("prot_level", protPart);
				armorDmgExpression.setVariable("unbr_level", unbrPart);
				targetHuman.getInventory().setLeggings(doItemDamage(leggings, armorDmgExpression.evaluate() * timeFactor));
			}
			if(boots != null) {
				double basePart = 0;
				if(armor.containsKey(boots.getType())) {
					basePart = armor.get(boots.getType());
				}
				double protPart = boots.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
				double unbrPart = boots.getEnchantmentLevel(Enchantment.DURABILITY);
				baseArmor += basePart;
				protLevel += protPart;
				armorDmgExpression.setVariable("base_armor", basePart);
				armorDmgExpression.setVariable("prot_level", protPart);
				armorDmgExpression.setVariable("unbr_level", unbrPart);
				targetHuman.getInventory().setBoots(doItemDamage(boots, armorDmgExpression.evaluate() * timeFactor));
			}
		}

		dmgExpression.setVariable("base_dmg", baseDmg);
		dmgExpression.setVariable("sharp_level", sharpLevel);
		dmgExpression.setVariable("strength_level", strengthLevel);
		dmgExpression.setVariable("weakness_level", weaknessLevel);
		dmgExpression.setVariable("other_level", otherLevel);
		dmgExpression.setVariable("base_armor", baseArmor);
		dmgExpression.setVariable("prot_level", protLevel);
		
		DamageManager.instance.doDamage(player, entity, DamageType.PHYSICAL, dmgExpression.evaluate() * timeFactor);
		
		// Fire aspect
		if(itemInHand != null) {
			final LivingEntity target = entity;
			final int fireLevel = itemInHand.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
			
			if(fireLevel > 0) {
				// for some reason Bukkit decides to not set targets on fire if you call setFireTicks
				// in the same tick that a damage event was cancelled; solution: delay by 1 tick
				Bukkit.getScheduler().scheduleSyncDelayedTask(AqPvP.instance, new Runnable() {
					public void run() {
						target.setFireTicks(80 * fireLevel);
					}
				});
			}
		}
		
		// Calculate weapon damage
		if(itemInHand != null && weapons.containsKey(itemInHand.getType()) && !player.getGameMode().equals(GameMode.CREATIVE)) {
			swordDmgExpression.setVariable("base_dmg", baseDmg);
			swordDmgExpression.setVariable("sharp_level", sharpLevel);
			swordDmgExpression.setVariable("strength_level", strengthLevel);
			swordDmgExpression.setVariable("weakness_level", weaknessLevel);
			swordDmgExpression.setVariable("other_level", otherLevel);
			swordDmgExpression.setVariable("base_armor", baseArmor);
			swordDmgExpression.setVariable("prot_level", protLevel);
			swordDmgExpression.setVariable("unbr_level", itemInHand.getEnchantmentLevel(Enchantment.DURABILITY));
			
			itemInHand = doItemDamage(itemInHand, swordDmgExpression.evaluate() * timeFactor);
			player.setItemInHand(itemInHand);
		}
		
		// Calculate knockback
		int knockbackLevel = itemInHand != null ? itemInHand.getEnchantmentLevel(Enchantment.KNOCKBACK) : 0;
		int sprinting = player.isSprinting() ? 1 : 0;
		knockbackExpression.setVariable("knockback_level", knockbackLevel);
		knockbackExpression.setVariable("sprinting", sprinting);
		double amplitude = knockbackExpression.evaluate() * timeFactor;
		if(entity instanceof Player && RuneManager.instance.hasRune((Player) entity, Rune.TENACITY)) {
			amplitude *= (1 - RuneManager.TENACITY_REDUCTION);
		}
		Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(amplitude);
		entity.setVelocity(entity.getVelocity().add(knockback));
	}
	
	// Calculates armor damage
	private ItemStack doItemDamage(ItemStack item, double damage) {
		if(item.getType().getMaxDurability() > 0) {
			item.setDurability((short) (item.getDurability() + randomRound(damage)));
			if(item.getDurability() > item.getType().getMaxDurability()) {
				return null;
			}
			else {
				return item;
			}
		}
		else {
			return item;
		}
	}
	
	// Durability is a short which doesn't have decimal places; armor damage needs some RNG
	private double randomRound(double damage) {
		// 1.4 damage = 2 damage (40% chance) or 1 damage (60% chance)
		double finalDamage = Math.floor(damage);
		finalDamage += Math.random() < (damage - finalDamage) ? 1 : 0;
		
		return finalDamage;
	}
	
	// Returns whether a player can attack a target
	public boolean attack(String player) {
		if(AqPvP.MELEE_MODE == AqPvP.MeleeMode.NEAR_TARGET) {
			if(cooldowns.containsKey(player)) {
				if(System.currentTimeMillis() < cooldowns.get(player)) {
					return false;
				}
			}

			cooldowns.put(player, System.currentTimeMillis() + ATTACK_COOLDOWN_MS);
		}
		else if(AqPvP.MELEE_MODE == AqPvP.MeleeMode.TIME_SCALE) {
			lastAttack.put(player, System.currentTimeMillis());
		}
		
		return true;
	}
	
	public long getLastAttack(String player) {
		if(lastAttack.containsKey(player)) {
			return System.currentTimeMillis() - lastAttack.get(player);
		}
		else {
			return Long.MAX_VALUE;
		}
	}
}
