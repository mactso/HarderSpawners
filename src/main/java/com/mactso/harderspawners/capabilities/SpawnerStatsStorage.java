package com.mactso.harderspawners.capabilities;

public class SpawnerStatsStorage implements ISpawnerStatsStorage {
	private int maxSpawnDelay;
	private int minSpawnDelay;
	private int durability;
	private boolean stunned;
	private boolean infinite;
	private boolean initialized = false;

	public SpawnerStatsStorage() {
		this.maxSpawnDelay = 1200;
		this.minSpawnDelay = 400;
		this.durability = -1;
		this.stunned = false;
		this.infinite = false;

	}

	@Override
	public int getMaxSpawnDelay() {
		return maxSpawnDelay;
	}

	@Override
	public int getMinSpawnDelay() {
		return minSpawnDelay;
	}

	@Override
	public int getDurability() {
		return durability;
	}

	@Override
	public void setMaxSpawnDelay(int amount) {
		this.maxSpawnDelay = amount;
	}

	@Override
	public void setMinSpawnDelay(int amount) {
		this.minSpawnDelay = amount;
	}

	@Override
	public void setDurability(int amount) {
		this.durability = amount;
	}

	@Override
	public boolean isStunned() {
		return stunned;
	}

	@Override
	public void setStunned(boolean bool) {
		stunned = bool;
	}

	@Override
	public boolean isInfiniteDurability() {
		return infinite;
	}

	@Override
	public void setInfinite(boolean bool) {
		infinite = bool;

	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void setInitialized(){
		initialized = true;
	}
}
