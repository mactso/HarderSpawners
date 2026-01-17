package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;

// this class handles finalizing spawner mob spawns 
public class ProcessMobSpawns {
	private final static int SECONDS_PER_TICK = 20;
	public static void processMobSpawn(Mob mob, EntitySpawnReason spawnReason) {
		// 1. Check if the creature is spawning from a spawner
		if (spawnReason != EntitySpawnReason.SPAWNER)
			return;


		// 2. Undead mobs burn in sunlight.  But don't waste cpu on fire resist buffs unless sun nearby.
		int skyLight = mob.level().getBrightness(LightLayer.SKY, mob.blockPosition());
		if ((skyLight > 0) || (mob.getType().is(EntityTypeTags.UNDEAD))) {
	
			// 3. Use MyUtility.updateEffect to give fire resistance
			// amplifier 0 = Level 1 effect
			int seconds = MyConfig.getHostileSpawnerResistDaylightDuration();
			int duration = seconds * SECONDS_PER_TICK;
			mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, true));
			MyUtilities.debugMsg(1, mob.blockPosition(), "Protected spawner mob from sunlight for: "+seconds+" seconds.");
		}
	}

}
