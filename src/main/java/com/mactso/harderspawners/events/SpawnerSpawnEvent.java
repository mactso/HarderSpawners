package com.mactso.harderspawners.events;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.config.MobSpawnerManager;
import com.mactso.harderspawners.config.MobSpawnerManager.MobSpawnerBreakPercentageItem;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnerSpawnEvent {
	private static int debugThreadIdentifier = 0;
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int SECONDS_120 = 2400;
	private static final int EFFECT_LEVEL_0 = 0;
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onCheckSpawnerSpawn(LivingSpawnEvent.CheckSpawn event) {
//    	ASM code needed here:
//
//    	package net.minecraft.entity.monster;
//    	public static boolean canMonsterSpawnInLight(EntityType<? extends MonsterEntity> type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
//    	     return worldIn.getDifficulty() != Difficulty.PEACEFUL && isValidLightLevel(worldIn, pos, randomIn) && canSpawnOn(type, worldIn, reason, pos, randomIn);
//    	}
//
//      Look at beekeeper
//	    

		// context - this event only happens once every 15 to 45 seconds per active
		// spawner.
		if (event.getSpawnReason() != MobSpawnType.SPAWNER) {
			return;
		}
		
		if (event.getSpawner() == null)
			return;
		
		if (event.getSpawner().getSpawnerBlockEntity() == null)  // Bumblezone, issue #8.
			return;
		

		
		if (MyConfig.debugLevel > 0) {
			debugThreadIdentifier = (debugThreadIdentifier + 1) % 10000;
			System.out.println("HarderSpawners: (" + debugThreadIdentifier + ") Checking Spawner Spawn Event at "
					+ (int) event.getX() + "+(int)event.getY()+" + (int) event.getZ() + ".");
		}
		if (!(event.getLevel() instanceof ServerLevel)) {
			return;
		}
		


		ServerLevel serverWorld = (ServerLevel) event.getLevel();
		
		LivingEntity le = event.getEntity();

		if (serverWorld.isUnobstructed(le)) {
			event.setResult(Result.ALLOW);  // prevent entity spawn rules from blocking it
		} else {
			event.setResult(Result.DENY);
			return;
		}
		
		boolean inWater = false;
		if (serverWorld.containsAnyLiquid(le.getBoundingBox())) {
			if (!le.canBreatheUnderwater()) {
				le.addEffect(new MobEffectInstance (MobEffects.WATER_BREATHING, MyConfig.getHostileSpawnerResistDaylightDuration()*20, EFFECT_LEVEL_0, false, false));
			}
		}

		// serverWorld.addBlockEntityTicker(null);
		// LevelTicks<Block> v = serverWorld.getBlockTicks();
		BlockPos eventPos = new BlockPos(Mth.floor(event.getX()), Mth.floor(event.getY()), Mth.floor(event.getZ()));

		int skylight = serverWorld.getBrightness(LightLayer.SKY, eventPos);
		if (skylight < 15) {
			if (serverWorld.getMaxLocalRawBrightness(eventPos) > 6)  {
				boolean destroyedLight = SharedUtilityMethods.removeLightNearSpawner(eventPos, serverWorld);
			}
		}


		BlockPos AbSpPos = event.getSpawner().getSpawnerBlockEntity().getBlockPos();
		BaseSpawner mySpawner = event.getSpawner();
		
		Utility.drawParticleBeam(AbSpPos.north(4), serverWorld, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()));
		Utility.drawParticleBeam(AbSpPos.south(4), serverWorld, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()));
		Utility.drawParticleBeam(AbSpPos.east(4), serverWorld, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()));
		Utility.drawParticleBeam(AbSpPos.west(4), serverWorld, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()));

		// mySpawner.spawnRange;
		int debug5 = 7;
		boolean justSpawned=true;
		updateHostileSpawnerValues(mySpawner, justSpawned);
		int debug = 4;

		String leStr = ForgeRegistries.ENTITY_TYPES.getKey(le.getType()).toString();
		MobSpawnerBreakPercentageItem t = MobSpawnerManager.getMobSpawnerBreakPercentage(leStr);

		if ((MyConfig.getHostileSpawnerResistDaylightDuration()>0) && (serverWorld.getMaxLocalRawBrightness(eventPos) > 8)) {
			le.addEffect(new MobEffectInstance (MobEffects.FIRE_RESISTANCE, MyConfig.getHostileSpawnerResistDaylightDuration()*20, EFFECT_LEVEL_0, false, false));
		}
		
		if (t == null) {
			leStr = "harderspawners:default";
			t = MobSpawnerManager.getMobSpawnerBreakPercentage(leStr);
		}

		double mobSpawnerBreakPercentage = 0.2; // 0.2%
		if (t != null) {
			mobSpawnerBreakPercentage = t.getSpawnerBreakPercentage();
		}

		if (mobSpawnerBreakPercentage == 0.0) {
			return;
		}

		mobSpawnerBreakPercentage = mobSpawnerBreakPercentage / 4; // called 4 times in tick it spawns.

		RandomSource chance = event.getLevel().getRandom();
		double failRoll = 100.0 * chance.nextDouble();
		boolean canExplode = true;
		// keep in mind default odds are 1/500 (so 0.2 is 0.2% chance, not a 20%).
		if (failRoll < mobSpawnerBreakPercentage) {

			double explodeRoll = 100.0 * chance.nextDouble();
			if (le instanceof Silverfish) {
				canExplode = false;
			}
			serverWorld.destroyBlock(AbSpPos, false);
			boolean explode = false;
			if ((canExplode) && ((explodeRoll) < MyConfig.spawnersExplodePercentage)) {
				explode = true;
			}
			
			
			if (explode) {
			    Vec3 explodepos = new Vec3(AbSpPos.getX(),AbSpPos.getY(),AbSpPos.getZ());
			    serverWorld.explode(null, new DamageSource("explosion").setExplosion(), null, explodepos.x, explodepos.y, explodepos.z, 6, true, Explosion.BlockInteraction.DESTROY);
			}
		}
	}

	// this adjusts hostile mob spawners.
	// this will need to add CustomSpawnData to overcome the light level rules.
	
	public static void updateHostileSpawnerValues(BaseSpawner mySpawner, boolean fromSpawnEvent) {
		// 18.1 FD: net/minecraft/world/level/BaseSpawner/f_45451_ /maxNearbyEntities
		CompoundTag tag = new CompoundTag ();
		boolean changed = false;
		tag = mySpawner.save(tag);
		CompoundTag spawnDataTag = tag.getCompound("SpawnData");
		
		CompoundTag entityTag = spawnDataTag.getCompound("entity");
		
		Optional<EntityType<?>> optional = EntityType.by(entityTag);
        if (optional.isEmpty()) {
        	return;
        }

		if (optional.get().getCategory() != MobCategory.MONSTER) {
			return;
		}
		if (tag.getInt("MaxNearbyEntities") != MyConfig.maxNearbyEntities) {
			tag.putInt("MaxNearbyEntities", MyConfig.maxNearbyEntities);
			changed = true;
		}
		if (tag.getInt("RequiredPlayerRange") != MyConfig.requiredPlayerRange) {
			tag.putInt("RequiredPlayerRange", MyConfig.requiredPlayerRange);
			changed = true;
		}
		if (tag.getInt("SpawnRange") != MyConfig.spawnRange) {
			tag.putInt("SpawnRange", MyConfig.spawnRange);
			changed = true;
		}

		if (fromSpawnEvent) {
			int time = tag.getInt("MinSpawnDelay");
			if (tag.getInt("MinSpawnDelay") >= MyConfig.getSpawnerMinutesStunned()*1200) {
				tag.putInt("MinSpawnDelay",200);
				tag.putInt("MaxSpawnDelay",800);
				tag.putInt("Delay",240);
				changed = true;
			}
		}

		int lightLevel = MyConfig.getHostileSpawnerLightLevel();
		CustomSpawnRules c = new SpawnData.CustomSpawnRules(new InclusiveRange<Integer>(0, lightLevel),new InclusiveRange<Integer>(0, lightLevel));
		SpawnData s = new SpawnData(entityTag, Optional.of(c));
		
		Optional<Tag> wSD = SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, s).result();
		if (wSD.isPresent()) {
			if (!spawnDataTag.equals(wSD.get())) {
				tag.put("SpawnData", wSD.get());
				changed = true;
			}
		}

//		try {
//	    tag.put("SpawnData", SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, s).result().orElseThrow(() -> {
//	          return new IllegalStateException("Invalid SpawnData");
//	       }));
//		}]
//		catch(Exception e) {
//			// leave the existing SpawnData alone.
//		}

		if (changed) {
			mySpawner.load(mySpawner.getSpawnerBlockEntity().getLevel(),mySpawner.getSpawnerBlockEntity().getBlockPos(),  tag);
			ServerTickHandler.addClientUpdate((ServerLevel)mySpawner.getSpawnerBlockEntity().getLevel(),mySpawner.getSpawnerBlockEntity().getBlockPos());
		}



//		// 18.1 FD: net/minecraft/world/level/BaseSpawner/f_45452_ /requiredPlayerRange
//		Field fieldRequiredPlayerRange = null;
//		try {
//			String name = ASMAPI.mapField("f_45452_");
//			fieldRequiredPlayerRange = BaseSpawner.class.getDeclaredField(name);
//			fieldRequiredPlayerRange.setAccessible(true);
//		} catch (Exception e) {
//			LOGGER.error("XX Unexpected Reflection Failure requiredPlayerRange");
//			return;
//		}
//		try {
//			fieldRequiredPlayerRange.setInt(mySpawner, MyConfig.requiredPlayerRange); 
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		return;

	}

}
