package com.mactso.harderspawners.modloader.spawnerstorage;

import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * Platform-agnostic helper for spawner stats.
 * Delegates all logic to SpawnerStatsAdapter.
 * 
 * For Fabric, the SpawnerBlockEntity must implement SpawnerStatsHolder via a Mixin.
 */
public final class SpawnerStatsHelper {

    private SpawnerStatsHelper() {} // prevent instantiation

    /**
     * Returns a wrapper around a spawner's stats.
     * Initializes them if needed.
     *
     * @param sbe The spawner block entity.
     * @return SpawnerStatsWrapper for this spawner, or null if no valid entity ID.
     */
    public static SpawnerStatsAdapter.SpawnerStatsWrapper getOrCreateStats(SpawnerBlockEntity sbe) {
        return SpawnerStatsAdapter.getOrCreateStats(sbe);
    }
}