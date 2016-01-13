package me.aqlow.spigot.pvp.ai;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_8_R3.EntityBlaze;
import net.minecraft.server.v1_8_R3.EntityCreature;
import net.minecraft.server.v1_8_R3.EntityCreeper;
import net.minecraft.server.v1_8_R3.EntityEnderman;
import net.minecraft.server.v1_8_R3.EntityEndermite;
import net.minecraft.server.v1_8_R3.EntityGhast;
import net.minecraft.server.v1_8_R3.EntityGuardian;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityIronGolem;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityOcelot;
import net.minecraft.server.v1_8_R3.EntityPig;
import net.minecraft.server.v1_8_R3.EntitySilverfish;
import net.minecraft.server.v1_8_R3.EntitySkeleton;
import net.minecraft.server.v1_8_R3.EntitySnowman;
import net.minecraft.server.v1_8_R3.EntitySpider;
import net.minecraft.server.v1_8_R3.EntityWitch;
import net.minecraft.server.v1_8_R3.EntityWither;
import net.minecraft.server.v1_8_R3.EntityWolf;
import net.minecraft.server.v1_8_R3.EntityZombie;
import net.minecraft.server.v1_8_R3.PathfinderGoal;
import net.minecraft.server.v1_8_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R3.PathfinderGoalLeapAtTarget;
import net.minecraft.server.v1_8_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R3.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_8_R3.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.server.v1_8_R3.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_8_R3.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;

@SuppressWarnings("unchecked")
public class AIUtil {
	private static Field goals = null;
	private static Constructor<? extends PathfinderGoal> spiderAttack = null;
	private static Constructor<? extends PathfinderGoal> guardianAttack = null;
	private static ArrayList<GoalUpdates<? extends EntityInsentient>> goalUpdates = new ArrayList<GoalUpdates<? extends EntityInsentient>>();
	static {
		try {
			goals = PathfinderGoalSelector.class.getDeclaredField("b");
			goals.setAccessible(true);
			for(Class<?> c : EntitySpider.class.getDeclaredClasses()) {
				if(PathfinderGoal.class.isAssignableFrom(c)) {
					spiderAttack = (Constructor<? extends PathfinderGoal>) c.getConstructors()[0];
					spiderAttack.setAccessible(true);
					break;
				}
			}
			for(Class<?> c : EntityGuardian.class.getDeclaredClasses()) {
				if(PathfinderGoal.class.isAssignableFrom(c)) {
					guardianAttack = (Constructor<? extends PathfinderGoal>) c.getConstructors()[0];
					guardianAttack.setAccessible(true);
					break;
				}
			}
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityBlaze>(EntityBlaze.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityCreeper>(EntityCreeper.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityEnderman>(EntityEnderman.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// TODO - fix
			goalUpdates.add(new GoalUpdates<EntityEndermite>(EntityEndermite.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(1, new PathfinderGoalFloat(creature));
					creature.goalSelector.a(2, new PathfinderGoalMeleeAttack((EntityEndermite) creature, 1.0D, false));
					creature.goalSelector.a(3, new PathfinderGoalRandomStroll((EntityEndermite) creature, 1.0D));
					creature.goalSelector.a(7, new PathfinderGoalLookAtPlayer(creature, EntityHuman.class, 8.0F));
					creature.goalSelector.a(8, new PathfinderGoalRandomLookaround(creature));
				}
			});
			
			// TODO - fix
			goalUpdates.add(new GoalUpdates<EntityGhast>(EntityGhast.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityGuardian>(EntityGuardian.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(4, guardianAttack.newInstance(creature));
				    PathfinderGoalMoveTowardsRestriction localPathfinderGoalMoveTowardsRestriction;
				    creature.goalSelector.a(5, localPathfinderGoalMoveTowardsRestriction = new PathfinderGoalMoveTowardsRestriction((EntityCreature) creature, 1.0D));
				    //creature.goalSelector.a(8, new PathfinderGoalLookAtPlayer(creature, EntityHuman.class, 8.0F));
				    //creature.goalSelector.a(8, new PathfinderGoalLookAtPlayer(creature, EntityGuardian.class, 12.0F, 0.01F));
				    //creature.goalSelector.a(9, new PathfinderGoalRandomLookaround(creature));
				    localPathfinderGoalMoveTowardsRestriction.a(3);
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityIronGolem>(EntityIronGolem.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityOcelot>(EntityOcelot.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntitySilverfish>(EntitySilverfish.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(1, new PathfinderGoalFloat(creature));
					creature.goalSelector.a(4, new PathfinderGoalMeleeAttack((EntitySilverfish) creature, 1.0D, false));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntitySkeleton>(EntitySkeleton.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntitySnowman>(EntitySnowman.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntitySpider>(EntitySpider.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(1, new PathfinderGoalFloat(creature));
					creature.goalSelector.a(3, new PathfinderGoalLeapAtTarget(creature, 0.4F));
					creature.goalSelector.a(4, spiderAttack.newInstance(creature, EntityLiving.class));
					creature.goalSelector.a(6, new PathfinderGoalLookAtPlayer(creature, EntityHuman.class, 8.0F));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityWitch>(EntityWitch.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityWither>(EntityWither.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityWolf>(EntityWolf.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
				}
			});
			
			// WORKING
			goalUpdates.add(new GoalUpdates<EntityZombie>(EntityZombie.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(0, new PathfinderGoalFloat(creature));
					creature.goalSelector.a(2, new PathfinderGoalMeleeAttack((EntityZombie) creature, 1.0D, false));
					creature.goalSelector.a(5, new PathfinderGoalMoveTowardsRestriction((EntityZombie) creature, 1.0D));
				}
			});
			
			// TODO - remove or add for all animals
			goalUpdates.add(new GoalUpdates<EntityPig>(EntityPig.class) {
				public void runUpdates(EntityInsentient creature) throws Exception {
					clearTargetSelector(creature);
					clearGoalSelector(creature);
					creature.goalSelector.a(-1, new AIPathfinderGoal(creature));
					creature.goalSelector.a(1, new PathfinderGoalFloat(creature));
					creature.goalSelector.a(4, new PathfinderGoalMeleeAttack((EntityPig) creature, 1.0D, false));
				}
			});
		}
		catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public static void updateSelectors(EntityInsentient entity) {
		for(GoalUpdates<? extends EntityInsentient> goalUpdate : goalUpdates) {
			if(goalUpdate.getEntityClass().isAssignableFrom(entity.getClass())) {
				try {
					goalUpdate.runUpdates(entity);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void clearTargetSelector(EntityInsentient entity) {
		try {
			List targetSelector = (List) goals.get(entity.targetSelector);
			targetSelector.clear();
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void clearGoalSelector(EntityInsentient entity) {
		try {
			List goalSelector = (List) goals.get(entity.goalSelector);
			goalSelector.clear();
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private abstract static class GoalUpdates<T extends EntityInsentient> {
		private Class<T> type;
		
		public GoalUpdates(Class<T> c) {
			this.type = c;
		}
		abstract void runUpdates(EntityInsentient entity) throws Exception;
		Class<T> getEntityClass() {
			return type;
		}
	}
}
