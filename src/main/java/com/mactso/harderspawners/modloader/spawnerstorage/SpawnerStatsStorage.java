package com.mactso.harderspawners.modloader.spawnerstorage;

import java.util.Optional;

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
	public void deserializeNBT(Optional<CompoundTag> optTag) {
	    if (optTag.isEmpty()) {
	        // No data present; use defaults
	        dataVersion = 0;
	        infinite = false;
	        stunned = false;
	        lifeSpanInTicks = 20L * 60L * 500L;
	        originalMinSpawnDelay = 200;
	        originalMaxSpawnDelay = 800;
	        originalEntityId = "";
	        initialized = false;
	        return;
	    }

	    CompoundTag tag = optTag.get();
	    Optional<Integer> optDataVersion = tag.getInt("DataVersion");
	    dataVersion = optDataVersion.orElse(0);

	    Optional<Boolean> optInfinite = tag.getBoolean("Infinite");
	    infinite = optInfinite.orElse(false);

	    Optional<Boolean> optStunned = tag.getBoolean("Stunned");
	    stunned = optStunned.orElse(false);

	    Optional<Long> optLifeSpan = tag.getLong("Lifespan");
	    lifeSpanInTicks = optLifeSpan.orElse(20L * 60L * 500L);

	    Optional<Integer> optOriginalMinSpawnDelay = tag.getInt("OriginalMinSpawnDelay");
	    originalMinSpawnDelay = optOriginalMinSpawnDelay.orElse(200);

	    Optional<Integer> optOriginalMaxSpawnDelay = tag.getInt("OriginalMaxSpawnDelay");
	    originalMaxSpawnDelay = optOriginalMaxSpawnDelay.orElse(800);

	    Optional<String> optOriginalEntityId = tag.getString("OriginalEntityId");
	    originalEntityId = optOriginalEntityId.orElse("").trim();

	    initialized = true; // class exists and read
	}

}

