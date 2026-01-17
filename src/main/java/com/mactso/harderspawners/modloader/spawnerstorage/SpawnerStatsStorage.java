package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.common.utility.SharedUtilityMethods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public class SpawnerStatsStorage implements ValueIOSerializable {


    // --- Core Fields ---
    private int durability = -1;
    private boolean stunned;
    private boolean infinite;
    private boolean initialized;

    // --- Timing ---
    private long spawnerExpirationTime = -1L;

    // Backup of original spawner data
    private CompoundTag originalTag;

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

    public CompoundTag getOriginalTag() { return originalTag; }

    // --- Logic ---
    public boolean hasExpired(SpawnerBlockEntity sbe) {
        if (infinite) return false;
        if (sbe == null || sbe.getLevel() == null) return false;

        ChunkAccess chunk = sbe.getLevel().getChunk(sbe.getBlockPos());
        return chunk.getInhabitedTime() > spawnerExpirationTime;
    }

    public long getAverageTimePerSpawn() {
        if (originalTag == null) return 0L;
        int minDelay = originalTag.getIntOr("MinSpawnDelay", 200);
        int maxDelay = originalTag.getIntOr("MaxSpawnDelay", 800);
        return ((long) minDelay + (long) maxDelay) / 2L;
    }

    public long getInitialFailureTime(long chunkInhabitedTime) {
        if (infinite) return Long.MAX_VALUE;
        return chunkInhabitedTime + ((long) durability * getAverageTimePerSpawn());
    }

    // --- Backup ---
    public void backupOriginalSpawner(SpawnerBlockEntity sbe) {
        if (originalTag != null) return;
        originalTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
    }

    // --- ValueIOSerializable implementation ---
    @Override
    public void serialize(ValueOutput output) {
        output.putInt("Durability", durability);
        output.putBoolean("Stunned", stunned);
        output.putBoolean("Infinite", infinite);
        output.putBoolean("Initialized", initialized);
        output.putLong("FailureGameTime", spawnerExpirationTime);
        if (originalTag != null) { // this was the spawner compound tag minecraft, or a mod, or a map had set.
        	output.storeNullable("OriginalTag", CompoundTag.CODEC, originalTag);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        durability = input.getIntOr("Durability", -1);
        stunned = input.getBooleanOr("Stunned", false);
        infinite = input.getBooleanOr("Infinite", false);
        initialized = input.getBooleanOr("Initialized", false);
        spawnerExpirationTime = input.getLongOr("FailureGameTime", -1L);
        originalTag = input.read("OriginalTag", CompoundTag.CODEC).orElse(null);
    }
}
