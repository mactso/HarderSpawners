package com.mactso.harderspawners.modloader.spawnerstorage;

import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * Platform-agnostic helper class for spawner logic.
 * No direct NeoForge calls here; everything goes through SpawnerStatsAdapter.
 */
public class SpawnerStatsHelper {

    /**
     * Returns a wrapper around a spawner's stats.
     * Initializes them if needed.
     */
    public static SpawnerStatsAdapter.SpawnerStatsWrapper getOrCreateStats(SpawnerBlockEntity sbe) {
        return SpawnerStatsAdapter.getOrCreateStats(sbe);
    }
}
