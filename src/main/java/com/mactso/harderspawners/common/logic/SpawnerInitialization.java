package com.mactso.harderspawners.common.logic;

import java.util.Optional;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerInitialization {

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

	public static void applyConfigToMonsterSpawners(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

		// Local debug level for testing
		int testingDebugLevel = 0;
//		// TODO this is disabled temporarily.
//		if (testingDebugLevel == 0) return;

		MyUtilities.debugMsg(testingDebugLevel,
				"Entering doApplyConfigToMonsterSpawners for spawner at " + sbe.getBlockPos());

		// Nested SpawnData inside the spawner
		CompoundTag spawnDataTag = spawnerTag.getCompound("SpawnData");
		if (spawnDataTag.isEmpty()) {
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag is empty, aborting.");
			return;
		}

		// Only apply to monster spawners
		if (!SpawnerUtilityMethods.isMonsterSpawner(sbe, spawnerTag)) {
			MyUtilities.debugMsg(testingDebugLevel, "Spawner is not a monster spawner, skipping.");
			return;
		}

		MyUtilities.debugMsg(testingDebugLevel, "Applying configuration overrides to spawner.");

		// Apply configuration overrides directly to the spawner tag
		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "MaxNearbyEntities", MyConfig.getMaxNearbyEntities());
		MyUtilities.debugMsg(testingDebugLevel, "MaxNearbyEntities set to " + MyConfig.getMaxNearbyEntities());

		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "RequiredPlayerRange", MyConfig.getRequiredPlayerRange());
		MyUtilities.debugMsg(testingDebugLevel, "RequiredPlayerRange set to " + MyConfig.getRequiredPlayerRange());

		SpawnerUtilityMethods.putIntIfDifferent(spawnerTag, "SpawnRange", MyConfig.getSpawnRange());
		MyUtilities.debugMsg(testingDebugLevel, "SpawnRange set to " + MyConfig.getSpawnRange());

		SpawnerInitialization.maybeOverrideSpawnDelays(spawnerTag);
		MyUtilities.debugMsg(testingDebugLevel, "Spawn delays processed with maybeOverrideSpawnDelays.");

		// Optionally rebuild SpawnData with custom light levels
		MyUtilities.debugMsg(0, "spawnerdatatag" + spawnerTag.getAsString());
		Optional<Tag> workSpawnData = SpawnerLightLogic.buildCustomLightLevelSpawnData(spawnDataTag);
		if (workSpawnData.isPresent() && !spawnDataTag.equals(workSpawnData.get())) {
			spawnerTag.put("SpawnData", workSpawnData.get());
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag updated with custom light level spawn data.");
		} else {
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag unchanged after custom light level processing.");
		}
		// ensureDefaultSpawnPotentials(spawnerTag);

		MyUtilities.debugMsg(0, "spawnerdatatag" + spawnerTag.getAsString());

		// Save back to spawner
		SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
		MyUtilities.debugMsg(testingDebugLevel, "Spawner NBT loaded back into spawner block entity.");
	}

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

	public static boolean isSpawnerDelayVanilla(CompoundTag tag) {
		if (!tag.contains("MinSpawnDelay") || !tag.contains("MaxSpawnDelay")) {
			return false;
		}

		int min = tag.getInt("MinSpawnDelay");
		int max = tag.getInt("MaxSpawnDelay");

		return min == 200 && max == 800;
	}

}
