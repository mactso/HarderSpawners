package com.mactso.harderspawners.common.utility;

import java.util.Optional;

import com.mactso.harderspawners.common.logic.ProcessSpawners;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
	 * Utility methods shared across the Harder Spawners mod.
	 * 
 * <p>
 * Includes helper functions for Spawners
 * 
 * <p>
 * <strong>Important:</strong> Some methods are computationally expensive but
	 * are safe because they are throttled:
	 * <ul>
 * <li>{@link #doDestroyLightingNearSpawner(BlockPos, ServerLevel)} is only
 * called when a spawner is about to spawn (spawnDelay == 1) or when a player
 * attempts to break a spawner.</li>
	 *   <li>This ensures that even with 3D scanning of nearby blocks, the performance
	 *       impact is limited.</li>
	 * </ul>
	 * </p>
	 */
public class SpawnerUtilityMethods {
	
	/*
	 * Debugging utility.  Command showspawnertag probably deprecates this.
	 */
	public static void logSpawnerState(
	        int level,
	        String context,
	        SpawnerBlockEntity sbe,
	        SpawnerStatsWrapper wrapper
	) {
	    if (sbe == null)
	        return;

	    BaseSpawner spawner = sbe.getSpawner();
	    CompoundTag spawnerTag = saveSpawnerToTag(sbe);
	    int delay = ProcessSpawners.getSpawnerDelay(sbe, spawner, spawnerTag);

	    MyUtilities.debugMsg(
	        level,
	        sbe.getBlockPos(),
	        context
	        + " Spawner state | life="
	        + (wrapper == null ? "null" : wrapper.getLifespan())
	        + " | stunned="
	        + (wrapper == null ? "null" : wrapper.isStunned())
	        + " | Orig [min="
	        + (wrapper == null ? "null" : wrapper.getOriginalMinSpawnDelay())
	        + ", max="
	        + (wrapper == null ? "null" : wrapper.getOriginalMaxSpawnDelay())
	        + ", delay="
	        + delay
	        + "] | NBT[min="
	        + spawnerTag.getInt("MinSpawnDelay")
	        + ", max="
	        + spawnerTag.getInt("MaxSpawnDelay")
	        + ", delay="
	        + spawnerTag.getInt("Delay")
	        + "]"
	    );
	}

	/** 
	 * Serializes a SpawnerBlockEntity into a CompoundTag. 
	 * @param sbe the spawner block entity 
	 * @return serialized NBT representing the spawner 
	 */
    public static CompoundTag saveSpawnerToTag(SpawnerBlockEntity sbe) {
        CompoundTag tag = new CompoundTag();
        sbe.getSpawner().save(tag);
        return tag;
    }
	
	/** 
	 * Loads spawner data from a CompoundTag into a SpawnerBlockEntity. 
	 * @param sbe target spawner 
	 * @param tag NBT to load 
	 */
    public static void loadSpawnerFromTag(SpawnerBlockEntity sbe, CompoundTag tag) {
        sbe.getSpawner().load(
            sbe.getLevel(),
            sbe.getBlockPos(),
            tag
        );
    }
	/** 
	 * Returns true if the given spawner is stunned according to its SpawnerStats. 
	 */
    public static boolean isSpawnerStunned(SpawnerBlockEntity sbe) {
        SpawnerStatsAdapter.SpawnerStatsWrapper wrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
        return wrapper != null && wrapper.isStunned();
    }

    public static int getIntOrDefault(CompoundTag tag, String key, int defaultValue) {
	    if (tag.contains(key)) {
	        return tag.getInt(key);
	    }
	    return defaultValue;
	}

	
	public static void putIntIfDifferent(CompoundTag tag, String key, int value) {
	    if (!tag.contains(key) || tag.getInt(key) != value) {
	        tag.putInt(key, value);
	    }
	}

	
	
	/**
	 * Returns true if the spawner contains a monster-type entity.
	 */
	public static boolean isMonsterSpawner(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

	    if (spawnerTag == null) {
	        return false;
	    }

	    CompoundTag spawnData = spawnerTag.getCompound("SpawnData");
	    if (spawnData == null || spawnData.isEmpty()) {
	        return false;
	    }

	    CompoundTag entityData = spawnData.getCompound("entity");
	    if (entityData == null || entityData.isEmpty()) {
	        return false;
	    }

	    String id = entityData.getString("id");
	    if (id == null || id.isBlank()) {
	        return false;
	    }

	    Optional<EntityType<?>> entityTypeOpt = EntityType.byString(id);
	    if (entityTypeOpt.isEmpty()) {
	        return false;
	    }

	    EntityType<?> entityType = entityTypeOpt.get();
	    return entityType.getCategory() == MobCategory.MONSTER;
	}
	
	public static String makeSpawnerCompoundTagReport(SpawnerBlockEntity sbe) {
	    if (sbe == null) return "<null spawner>";

	    CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
	    BlockPos pos = sbe.getBlockPos();

	    StringBuilder sb = new StringBuilder();
	    sb.append("Spawner @ ").append(pos).append("\n");
	    sb.append("{\n");

	    for (String key : spawnerTag.getAllKeys()) {
	        Tag value = spawnerTag.get(key);

	        // ---- formatting rules ----
	        if ("entity".equals(key) || "custom_spawn_rules".equals(key)) {
	            sb.append("\n");
	        }

	        sb.append("  ")
	          .append(key)
	          .append(" = ")
	          .append(value)
	          .append("\n");
	    }

	    sb.append("}");
	    return sb.toString();
	}

	public static boolean isTrialSpawner(SpawnerBlockEntity sbe) {
		CompoundTag tag = saveSpawnerToTag(sbe);
		return tag.contains("TrialRoomID") || tag.getString("id").startsWith("trialmod:");
	}
	

}
