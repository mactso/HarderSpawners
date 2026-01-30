package com.mactso.harderspawners.common.logic;

import java.util.Optional;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/*
* Handles initialization and configuration of monster spawners.
* Ensures default spawn potentials, applies mod config, and overrides delays.
* Intended for hostile mob spawners in HarderSpawners.
*/
public class SpawnerInitialization {

    /*
    * Ensures SpawnPotentials exists if SpawnData is defined.
    * Converts single SpawnData to a weighted list for vanilla compliance.
    * @Deprecated Written to try to fix a bug. didn't work.  may use in future.
    */
	public static void ensureDefaultSpawnPotentials(CompoundTag spawnerTag) {
		if (spawnerTag.contains("SpawnPotentials", Tag.TAG_LIST)
				&& spawnerTag.getList("SpawnPotentials", Tag.TAG_COMPOUND).isEmpty()) {

			CompoundTag spawnData = spawnerTag.getCompound("SpawnData");
			if (!spawnData.isEmpty()) {
				ListTag list = new ListTag();

				CompoundTag entry = new CompoundTag();
				entry.putInt("weight", 1);
				entry.put("data", spawnData.copy());

				list.add(entry);
				spawnerTag.put("SpawnPotentials", list);
			}
		}

	}

    /*
    * Applies mod configuration to monster spawners only.
    * Overrides nearby entity count, player range, spawn range, and optionally delays.
    * Rebuilds SpawnData with custom light levels if required.
    */
	public static void applyConfigToMonsterSpawners(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1,
				"Entering doApplyConfigToMonsterSpawners for spawner at " + sbe.getBlockPos());

		// Nested SpawnData inside the spawner
		CompoundTag spawnDataTag = spawnerTag.getCompound("SpawnData");
		if (spawnDataTag.isEmpty()) 
			return;

		// Only apply to monster spawners
		if (!SpawnerUtilityMethods.isMonsterSpawner(sbe, spawnerTag)) 
			return;

		// Apply configuration overrides directly to the spawner tag
		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "MaxNearbyEntities", MyConfig.getMaxNearbyEntities());
		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "RequiredPlayerRange", MyConfig.getRequiredPlayerRange());
		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "SpawnRange", MyConfig.getSpawnRange());
		SpawnerInitialization.maybeOverrideSpawnDelays(spawnerTag);

		// Optionally rebuild SpawnData with custom light levels
		MyUtilities.debugMsg(0, "spawnerdatatag" + spawnerTag.getAsString());
		Optional<Tag> workSpawnData = SpawnerLightLogic.buildCustomLightLevelSpawnData(spawnDataTag);
		if (workSpawnData.isPresent() && !spawnDataTag.equals(workSpawnData.get())) {
			spawnerTag.put("SpawnData", workSpawnData.get());
		} else {
		}
		// Save back to spawner
		SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
	}

    /*
    * Optionally overrides vanilla spawner delays if allowed by config.
    * Preserves non-vanilla timings if configured.
    * Sets MinSpawnDelay and MaxSpawnDelay from config otherwise.
    */
	public static void maybeOverrideSpawnDelays(CompoundTag tag) {

		int testingDebugLevel = 0;

		// Check if both min/max are vanilla
		boolean isVanilla = SpawnerInitialization.isSpawnerDelayVanilla(tag);

		// Preserve check
		if (MyConfig.isPreserveNonVanillaSpawnerTiming() && !isVanilla) {
			MyUtilities.debugMsg(testingDebugLevel,
					"PreserveNonVanillaSpawnerTiming is true and delays are non-vanilla, skipping override.");
			return;
		}
		MyUtilities.debugMsg(testingDebugLevel, "Overriding vanilla spawner delays.");

		SpawnerUtilityMethods.putIntIfDifferent(tag, "MinSpawnDelay", MyConfig.getMinSpawnDelayOverride());
		MyUtilities.debugMsg(testingDebugLevel, "MinSpawnDelay overridden to " + MyConfig.getMinSpawnDelayOverride());

		SpawnerUtilityMethods.putIntIfDifferent(tag, "MaxSpawnDelay", MyConfig.getMaxSpawnDelayOverride());
		MyUtilities.debugMsg(testingDebugLevel, "MaxSpawnDelay overridden to " + MyConfig.getMaxSpawnDelayOverride());

	}

    /*
    * Checks if the spawner’s Min/Max delays match vanilla values (200-800).
    * Returns true if delays are vanilla, false otherwise.
    */
	public static boolean isSpawnerDelayVanilla(CompoundTag tag) {
		if (!tag.contains("MinSpawnDelay") || !tag.contains("MaxSpawnDelay")) {
			return false;
		}

		int min = tag.getInt("MinSpawnDelay");
		int max = tag.getInt("MaxSpawnDelay");

		return min == 200 && max == 800;
	}

}
