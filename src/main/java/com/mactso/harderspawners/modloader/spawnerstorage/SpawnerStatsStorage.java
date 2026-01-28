package com.mactso.harderspawners.modloader.spawnerstorage;

import net.minecraft.nbt.CompoundTag;

public class SpawnerStatsStorage {

	// --- Versioning ---
	private static final int CURRENT_DATA_VERSION = 2; // bumped version for entity ID support
	private int dataVersion = 0; // old spawners will deserialize as 0

	// --- Original entity ID ---
	private String originalEntityId = "";

	// --- Cached original spawn delays ---
	private int originalMinSpawnDelay = 200;
	private int originalMaxSpawnDelay = 800;

	public String getOriginalEntityId() {
		return originalEntityId;
	}

	public void setOriginalEntityId(String originalEntityId) {
		this.originalEntityId = originalEntityId;
	}

	public int getOriginalMinSpawnDelay() {
		return originalMinSpawnDelay;
	}

	public void setOriginalMinSpawnDelay(int originalMinSpawnDelay) {
		this.originalMinSpawnDelay = originalMinSpawnDelay;
	}

	public int getOriginalMaxSpawnDelay() {
		return originalMaxSpawnDelay;
	}

	public void setOriginalMaxSpawnDelay(int originalMaxSpawnDelay) {
		this.originalMaxSpawnDelay = originalMaxSpawnDelay;
	}

	public long getLifeSpan() {
		return lifeSpanInTicks;
	}

	public void setLifeSpan(long lifeSpanInTicks) {
		this.lifeSpanInTicks = lifeSpanInTicks;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized() {
		this.initialized = true;
	}

	// --- Core Fields ---
	private long lifeSpanInTicks = -1;
	private boolean infinite = false;
	private boolean stunned;
	private boolean initialized;

	// --- Accessors ---

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

	// --- Serialization ---
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

	// --- Deserialization ---
	public void deserializeNBT(CompoundTag tag) {

	    this.dataVersion = tag.contains("DataVersion") ? tag.getInt("DataVersion") : 0;		
		this.infinite = tag.contains("Infinite") ? tag.getBoolean("Infinite") : false;
		this.stunned = tag.contains("Stunned") ? tag.getBoolean("Stunned") : false;
		this.lifeSpanInTicks = tag.contains("Lifespan") ? tag.getLong("Lifespan") : 20L * 60L * 500L;
		this.originalMinSpawnDelay = tag.contains("OriginalMinSpawnDelay") ? tag.getInt("OriginalMinSpawnDelay") : 200;
		this.originalMaxSpawnDelay = tag.contains("OriginalMaxSpawnDelay") ? tag.getInt("OriginalMaxSpawnDelay") : 800;
		this.originalEntityId = tag.contains("OriginalEntityId") ? tag.getString("OriginalEntityId") : "";
		this.initialized = true; // class exists and read

	}

}

