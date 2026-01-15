package com.mactso.harderspawners.modloader.spawnerstorage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * NeoForge replacement for the old Forge capability SpawnerStatsStorage.
 * Stores per-spawner stats like durability, stun state, processed time, failure time,
 * and a backup of the original spawner NBT.
 */
public class SpawnerStatsStorage implements INBTSerializable<CompoundTag> {

    // --- Core Fields ---
    private int durability = -1;
    private boolean stunned = false;
    private boolean infinite = false;
    private boolean initialized = false;

    // --- Timing Fields ---
    private long spawnerExpirationTime = -1L;   // when the spawner should "fail"

    // Backup of the original spawner NBT for resets
    private CompoundTag originalTag = null;

    // --- Accessors ---
    public int getDurability() { return durability; }
    public void setDurability(int durability) { this.durability = durability; }

    public boolean isStunned() { return stunned; }
    public void setStunned(boolean stunned) { this.stunned = stunned; }

    public boolean isInfinite() { return infinite; }
    public void setInfinite(boolean infinite) { this.infinite = infinite; }

    public boolean isInitialized() { return initialized; }
    public void setInitialized() { this.initialized = true; }

    public long getSpawnerExpirationTime() { return spawnerExpirationTime; }
    public void setSpawnerExpirationTime(long time) { this.spawnerExpirationTime = time; }
    
    public boolean hasExpired(SpawnerBlockEntity sbe) {
        if (infinite) return false; // Infinite spawners never fail
        if (sbe == null || sbe.getLevel() == null) return false;

        ChunkAccess chunk = sbe.getLevel().getChunk(sbe.getBlockPos());
        return chunk.getInhabitedTime() > spawnerExpirationTime;
    }

    /**
     * Returns the average time per spawn based on the spawner's saved original tag.
     * Uses (MinSpawnDelay + MaxSpawnDelay) / 2.
     * Returns 0 if originalTag is null or missing delay values.
     */
    public long getAverageTimePerSpawn() {
        if (originalTag == null) {
            return 0L;
        }
        int minDelay = originalTag.getInt("MinSpawnDelay");
        int maxDelay = originalTag.getInt("MaxSpawnDelay");
        long avg = ((long) minDelay + (long) maxDelay) / 2L;
        return avg;
    }

    /**
     * Calculates the spawner failure time based on durability and average spawn duration.
     * 
     * @param chunkInhabitedTime The current inhabited time of the chunk in ticks
     * @return The calculated failure time in ticks, or Long.MAX_VALUE if infinite
     */
    public long getInitialFailureTime(long chunkInhabitedTime) {
        if (infinite) {
            return Long.MAX_VALUE;
        }
        long avgTime = getAverageTimePerSpawn();
        return chunkInhabitedTime + ((long) durability * avgTime);
    }
    
    public CompoundTag getOriginalTag() { return originalTag; }

    /**
     * Backup the spawner's original NBT if not already done.
     */
    public void backupOriginalSpawner(SpawnerBlockEntity sbe) {
        if (originalTag == null) {
            CompoundTag tag = new CompoundTag();
            sbe.getSpawner().save(tag);
            originalTag = tag;
        }
    }



    // --- NBT serialization ---
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Durability", durability);
        tag.putBoolean("Stunned", stunned);
        tag.putBoolean("Infinite", infinite);
        tag.putBoolean("Initialized", initialized);
        tag.putLong("FailureGameTime", spawnerExpirationTime);
        if (originalTag != null) tag.put("OriginalTag", originalTag.copy());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        durability = nbt.getInt("Durability");
        stunned = nbt.getBoolean("Stunned");
        infinite = nbt.getBoolean("Infinite");
        initialized = nbt.getBoolean("Initialized");
        spawnerExpirationTime = nbt.getLong("FailureGameTime");
        if (nbt.contains("OriginalTag"))
            originalTag = nbt.getCompound("OriginalTag").copy();
    }
}