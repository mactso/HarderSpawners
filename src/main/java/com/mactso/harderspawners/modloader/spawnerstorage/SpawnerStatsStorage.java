package com.mactso.harderspawners.modloader.spawnerstorage;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpawnerStatsStorage implements ValueIOSerializable {

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

	// --- ValueIOSerializable ---
	@Override
	public void serialize(ValueOutput output) {
		output.putInt("DataVersion", dataVersion);
		output.putBoolean("Initialized", initialized);
		output.putString("OriginalEntityId", originalEntityId);
		output.putBoolean("Stunned", stunned);
		output.putLong("Lifespan", lifeSpanInTicks);
		output.putBoolean("Infinite", infinite);
		output.putInt("OriginalMinSpawnDelay", originalMinSpawnDelay);
		output.putInt("OriginalMaxSpawnDelay", originalMaxSpawnDelay);

	}

	@Override
	public void deserialize(ValueInput input) {
		dataVersion = input.getIntOr("DataVersion", 0);
	    initialized = input.getBooleanOr("Initialized", true); // but should always be true;
		originalEntityId = input.getStringOr("OriginalEntityId", "");
		stunned = input.getBooleanOr("Stunned", false);
		originalMinSpawnDelay = input.getIntOr("OriginalMinSpawnDelay", 200);
		originalMaxSpawnDelay = input.getIntOr("OriginalMaxSpawnDelay", 800);

		// --- Migration for old spawners ---  Extra legacy data cleanup and initialization.
		if (dataVersion < CURRENT_DATA_VERSION) {
			dataVersion = CURRENT_DATA_VERSION;
	        lifeSpanInTicks = input.getLongOr("Lifespan", -1);
			infinite = input.getBooleanOr("Infinite", false);
			if (infinite) {
				lifeSpanInTicks = Long.MAX_VALUE;
			}
			if (lifeSpanInTicks == -1) {
				lifeSpanInTicks = 600L * ((originalMinSpawnDelay + originalMaxSpawnDelay) / 2L); // default 600 spawns
			}
		} else {
			lifeSpanInTicks = input.getLongOr("Lifespan", 0L);
			infinite = input.getBooleanOr("Infinite", false);
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
