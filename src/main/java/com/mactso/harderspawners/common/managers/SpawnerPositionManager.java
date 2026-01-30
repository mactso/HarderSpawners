package com.mactso.harderspawners.common.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerPositionManager {

	static final Map<ResourceKey<Level>, Set<BlockPos>> spawnerLocations = new ConcurrentHashMap<>();

	public static void clearSpawnerLocations() {
	    spawnerLocations.clear();
	}

	public static void forgetSpawner(Level level, BlockPos pos) {
		if (level == null || pos == null)
			return;

	    Set<BlockPos> locations = spawnerLocations.get(level.dimension());
		if (locations == null)
			return;

	    locations.remove(pos);

	    // Optional: clean up empty sets
	    if (locations.isEmpty()) {
	        spawnerLocations.remove(level.dimension());
	    }
	}
	
	public static void recordSpawnerPos(SpawnerBlockEntity sbe) {

		if (sbe == null) {
	        return;
	    }
        if (SpawnerUtilityMethods.isTrialSpawner(sbe)) {
           	return;
        }

	    Level level = sbe.getLevel();
	    if (level == null) {
	        return;
	    }
	
	    ResourceKey<Level> dimension = level.dimension();
	    BlockPos pos = sbe.getBlockPos();
	
		spawnerLocations.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet()).add(pos);
	}

	public static boolean isSpawnerNearby(Level level, BlockPos pos, int range) {
	
	    if (level == null) {
	        return false;
	    }
	
	    Set<BlockPos> locations = spawnerLocations.get(level.dimension());
	    if (locations == null) {
	        return false;
	    }
	
	    for (BlockPos spawnerPos : locations) {
            if (isWithinRange(pos, spawnerPos, range)) {
                return true;
            }
	    }
	
	    return false;
	}
	
    private static boolean isWithinRange(BlockPos a, BlockPos b, int range) {
		int dx = Math.abs(a.getX() - b.getX());
		int dy = Math.abs(a.getY() - b.getY());
		int dz = Math.abs(a.getZ() - b.getZ());

		if (dx > range)
			return false;
		if (dy > range)
			return false;
		if (dz > range)
			return false;

		return true;
    }
    
    /**
	 * Returns up to 'maxCount' tracked spawner positions within a certain range of
	 * a reference position in the given level.
     *
     * @param level the level/dimension to query
     * @param referencePos the position to measure distance from
     * @param range maximum distance in blocks along each axis
     * @param maxCount maximum number of spawner positions to return
     * @return a list of nearby spawner positions, up to maxCount
     */
    public static List<BlockPos> getNearbySpawners(Level level, BlockPos referencePos, int range, int maxCount) {
        if (level == null || referencePos == null || maxCount <= 0 || range < 0) {
            return List.of();
        }

        Set<BlockPos> locations = spawnerLocations.get(level.dimension());
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }

        List<BlockPos> nearby = new ArrayList<>(Math.min(maxCount, locations.size()));
        int count = 0;

        for (BlockPos pos : locations) {
            if (Math.abs(pos.getX() - referencePos.getX()) <= range
                    && Math.abs(pos.getY() - referencePos.getY()) <= range
                    && Math.abs(pos.getZ() - referencePos.getZ()) <= range) {
                nearby.add(pos);
                count++;
				if (count >= maxCount)
					break;
            }
        }

        return nearby;
    }

}
