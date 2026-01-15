package com.mactso.harderspawners.common.logic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerRegistry {

	static final Map<ResourceKey<Level>, Set<BlockPos>> spawnerLocations =
	new ConcurrentHashMap<>();

	public static void clearSpawnerLocations() {
	    spawnerLocations.clear();
	}

	public static void forgetSpawner(Level level, BlockPos pos) {
	    if (level == null || pos == null) return;

	    Set<BlockPos> locations = spawnerLocations.get(level.dimension());
	    if (locations == null) return;

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
	
	    Level level = sbe.getLevel();
	    if (level == null) {
	        return;
	    }
	
	    ResourceKey<Level> dimension = level.dimension();
	    BlockPos pos = sbe.getBlockPos();
	
	    spawnerLocations
	            .computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet())
	            .add(pos);
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
        return Math.abs(a.getX() - b.getX()) <= range
            && Math.abs(a.getY() - b.getY()) <= range
            && Math.abs(a.getZ() - b.getZ()) <= range;
    }

}
