package com.mactso.harderspawners.modloader.spawnerstorage;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SpawnerStatsStorage {

	// --- Versioning ---
	private static final int CURRENT_DATA_VERSION = 2;
	private int dataVersion = 0;

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

	/* ---------------- Accessors ---------------- */

	public long getLifespan() {
		return lifeSpanInTicks;
	}

	public void setLifespan(long ticks) {
		this.lifeSpanInTicks = ticks;
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
		if (infinite) {
			lifeSpanInTicks = Long.MAX_VALUE;
		}
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

	public void setOriginalMinSpawnDelay(int value) {
		this.originalMinSpawnDelay = value;
	}

	public int getOriginalMaxSpawnDelay() {
		return originalMaxSpawnDelay;
	}

	public void setOriginalMaxSpawnDelay(int value) {
		this.originalMaxSpawnDelay = value;
	}

	public String getOriginalEntityId() {
		return originalEntityId;
	}

	public void setOriginalEntityId(String id) {
		this.originalEntityId = id != null ? id : "";
	}

	/* ---------------- Logic ---------------- */

	public boolean hasExpired() {
		return !infinite && lifeSpanInTicks <= 0;
	}

	public void decrementLifespan(long ticks) {
		if (!infinite && lifeSpanInTicks > 0) {
			lifeSpanInTicks -= ticks;
			if (lifeSpanInTicks < 0) {
				lifeSpanInTicks = 0;
			}
		}
	}

	/* ---------------- Serialization (1.21.6) ---------------- */

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

	public void deserialize(ValueInput input) {
		dataVersion = input.getIntOr("DataVersion", 0);
		initialized = input.getBooleanOr("Initialized", false);
		originalEntityId = input.getStringOr("OriginalEntityId", "");
		stunned = input.getBooleanOr("Stunned", false);
		originalMinSpawnDelay = input.getIntOr("OriginalMinSpawnDelay", 200);
		originalMaxSpawnDelay = input.getIntOr("OriginalMaxSpawnDelay", 800);

		if (dataVersion < CURRENT_DATA_VERSION) {
			lifeSpanInTicks = input.getLongOr("Lifespan", -1);
			infinite = input.getBooleanOr("Infinite", false);

			if (infinite) {
				lifeSpanInTicks = Long.MAX_VALUE;
			} else if (lifeSpanInTicks == -1) {
				// legacy default: ~600 spawns
				lifeSpanInTicks = 600L * ((originalMinSpawnDelay + originalMaxSpawnDelay) / 2L);
			}

			dataVersion = CURRENT_DATA_VERSION;
		} else {
			lifeSpanInTicks = input.getLongOr("Lifespan", 0L);
			infinite = input.getBooleanOr("Infinite", false);
		}

		// --- Invariant enforcement ---
		if (originalEntityId.isBlank()) {
			initialized = false;
			stunned = false;
			infinite = false;
			lifeSpanInTicks = 0;
		}
	}
}
