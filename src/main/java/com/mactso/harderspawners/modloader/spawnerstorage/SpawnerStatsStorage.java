package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.common.utility.MyUtilities;
import net.minecraft.nbt.CompoundTag;

/**
 * Storage for per-spawner stats: lifespan, stun state, infinite state,
 * cached spawn delays, and original entity ID.
 * 
 * This is now used in Fabric via a Mixin into SpawnerBlockEntity.
 */
public class SpawnerStatsStorage {

    // --- Versioning ---
    private static final int CURRENT_DATA_VERSION = 2; // bumped version for entity ID support
    private int dataVersion = 0; // old spawners will deserialize as 0

    // --- Core Fields ---
    private long lifeSpanInTicks = -1;
    private boolean stunned;
    private boolean infinite;
    private boolean initialized;

    // --- Cached original spawn delays ---
    private int originalMinSpawnDelay = 200;
    private int originalMaxSpawnDelay = 800;

    // --- Original entity ID ---
    private String originalEntityId = "";

    // --- Accessors ---
    public long getLifespan() { return lifeSpanInTicks; }
    public void setLifespan(long lifeSpan) { this.lifeSpanInTicks = lifeSpan; }

    public boolean isStunned() { return stunned; }
    public void setStunned(boolean stunned) { this.stunned = stunned; }

    public boolean isInfinite() { return infinite; }
    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
        if (infinite) lifeSpanInTicks = Long.MAX_VALUE;
    }

    public boolean isInitialized() { return initialized; }
    public void setInitialized() {
        this.initialized = true;
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public int getOriginalMinSpawnDelay() { return originalMinSpawnDelay; }
    public void setOriginalMinSpawnDelay(int minDelay) { this.originalMinSpawnDelay = minDelay; }

    public int getOriginalMaxSpawnDelay() { return originalMaxSpawnDelay; }
    public void setOriginalMaxSpawnDelay(int maxDelay) { this.originalMaxSpawnDelay = maxDelay; }

    public String getOriginalEntityId() { return originalEntityId; }
    public void setOriginalEntityId(String entityId) { this.originalEntityId = entityId != null ? entityId : ""; }

    public int getDataVersion() { return dataVersion; }
    public void setDataVersion(int version) { this.dataVersion = version; }

    // --- Logic ---
    public boolean hasExpired() { return !infinite && lifeSpanInTicks <= 0; }

    public void decrementLifespan(long ticks) {
        if (!infinite && lifeSpanInTicks > 0) {
            lifeSpanInTicks -= ticks;
            if (lifeSpanInTicks < 0) lifeSpanInTicks = 0;
        }
    }

    // --- Serialization / Deserialization ---
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", dataVersion);
        tag.putBoolean("Initialized", initialized);
        tag.putString("OriginalEntityId", originalEntityId);
        tag.putBoolean("Stunned", stunned);
        tag.putLong("Lifespan", lifeSpanInTicks);
        tag.putBoolean("Infinite", infinite);
        tag.putInt("OriginalMinSpawnDelay", originalMinSpawnDelay);
        tag.putInt("OriginalMaxSpawnDelay", originalMaxSpawnDelay);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        // --- Version ---
        dataVersion = nbt.contains("DataVersion") ? nbt.getInt("DataVersion") : 0;

        // --- Core fields with defaults ---
        initialized = !nbt.contains("Initialized") || nbt.getBoolean("Initialized");
        originalEntityId = nbt.contains("OriginalEntityId") ? nbt.getString("OriginalEntityId") : "";
        stunned = nbt.contains("Stunned") && nbt.getBoolean("Stunned");
        originalMinSpawnDelay = nbt.contains("OriginalMinSpawnDelay") ? nbt.getInt("OriginalMinSpawnDelay") : 200;
        originalMaxSpawnDelay = nbt.contains("OriginalMaxSpawnDelay") ? nbt.getInt("OriginalMaxSpawnDelay") : 800;

        // --- Migration for old spawners ---
        if (dataVersion < CURRENT_DATA_VERSION) {
            dataVersion = CURRENT_DATA_VERSION;
            lifeSpanInTicks = nbt.contains("Lifespan") ? nbt.getLong("Lifespan") : -1L;
            infinite = nbt.contains("Infinite") && nbt.getBoolean("Infinite");

            if (infinite) lifeSpanInTicks = Long.MAX_VALUE;
            if (lifeSpanInTicks == -1)
                lifeSpanInTicks = 600L * ((originalMinSpawnDelay + originalMaxSpawnDelay) / 2L);
        } else {
            lifeSpanInTicks = nbt.contains("Lifespan") ? nbt.getLong("Lifespan") : 0L;
            infinite = nbt.contains("Infinite") && nbt.getBoolean("Infinite");
        }

        // --- Invariant enforcement (legacy-only failure) ---
        if (originalEntityId.isBlank()) {
            initialized = false;
            stunned = false;
            infinite = false;
            lifeSpanInTicks = 0;
            MyUtilities.debugMsg(0, "(Warn) SpawnerStatsStorage: Loaded spawner and it had no Entity Id.");
        }
    }
}