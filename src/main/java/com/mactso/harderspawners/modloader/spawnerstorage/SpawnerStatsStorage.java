package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.common.utility.MyUtilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * NeoForge replacement for the old Forge capability SpawnerStatsStorage.
 * Stores per-spawner stats like durability, stun state, processed time, failure time,
 * and a backup of the original spawner NBT.
 */
public class SpawnerStatsStorage implements INBTSerializable<CompoundTag> {

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
	public long getLifespan() {
		return lifeSpanInTicks;
	}

	public void setLifespan(long lifeSpan) {
		this.lifeSpanInTicks = lifeSpan;
	}

	public boolean isStunned() {
		return stunned;
    }

	public void setStunned(boolean stunned) {
		this.stunned = stunned;
        }

	public boolean isInfinite() {
		return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
        if (infinite)
            lifeSpanInTicks = Long.MAX_VALUE;
    }

	public boolean isInitialized() {
		return initialized;
        }

	public void setInitialized() {
		this.initialized = true;
        this.dataVersion = CURRENT_DATA_VERSION;
    }
    
	public int getOriginalMinSpawnDelay() {
		return originalMinSpawnDelay;
	}

	public void setOriginalMinSpawnDelay(int minDelay) {
		this.originalMinSpawnDelay = minDelay;
	}

	public int getOriginalMaxSpawnDelay() {
		return originalMaxSpawnDelay;
	}

	public void setOriginalMaxSpawnDelay(int maxDelay) {
		this.originalMaxSpawnDelay = maxDelay;
        }

	public String getOriginalEntityId() {
		return originalEntityId;
	}

	public void setOriginalEntityId(String entityId) {
		this.originalEntityId = entityId != null ? entityId : "";
    }

	public int getDataVersion() {
		return dataVersion;
	}

	public void setDataVersion(int version) {
		this.dataVersion = version;
	}
	
	    // --- Logic ---
    public boolean hasExpired() {
        return !infinite && lifeSpanInTicks <= 0;
    }
    
    public void decrementLifespan(long ticks) {
        if (!infinite && lifeSpanInTicks > 0) {
            lifeSpanInTicks -= ticks;
			if (lifeSpanInTicks < 0)
				lifeSpanInTicks = 0;
        }
    }
    
    // --- Store
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
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

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        dataVersion = nbt.getInt("DataVersion").orElse(0);

        initialized = nbt.getBoolean("Initialized").orElse(true);
        originalEntityId = nbt.getString("OriginalEntityId").orElse("");
        stunned = nbt.getBoolean("Stunned").orElse(false);
        originalMinSpawnDelay = nbt.getInt("OriginalMinSpawnDelay").orElse(200);
        originalMaxSpawnDelay = nbt.getInt("OriginalMaxSpawnDelay").orElse(800);

		// --- Migration for old spawners ---  Extra legacy data cleanup and initialization.
        if (dataVersion < CURRENT_DATA_VERSION) {
            dataVersion = CURRENT_DATA_VERSION;
            lifeSpanInTicks = nbt.getLong("Lifespan").orElse(-1L);
            infinite = nbt.getBoolean("Infinite").orElse(false);
            if (infinite) {
                lifeSpanInTicks = Long.MAX_VALUE;
            }

            if (lifeSpanInTicks == -1) {
				lifeSpanInTicks = 600L * ((originalMinSpawnDelay + originalMaxSpawnDelay) / 2L); // default 600 spawns
            }
        } else {
            lifeSpanInTicks = nbt.getLong("Lifespan").orElse(0L);
            infinite = nbt.getBoolean("Infinite").orElse(false);
        }

	    // --- Invariant enforcement ---
	    if (originalEntityId == null || originalEntityId.isBlank()) {
            initialized = false;
            stunned = false;
            infinite = false;
            lifeSpanInTicks = 0;
	        MyUtilities.debugMsg(  0,  "(Warn) SpawnerStatsStorage: Loaded spawner and it had no Entity Id.");
        }
    }
}
