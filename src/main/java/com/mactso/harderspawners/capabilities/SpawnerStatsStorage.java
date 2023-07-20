package com.mactso.harderspawners.capabilities;

public class SpawnerStatsStorage implements ISpawnerStatsStorage
{
	private int maxSpawnDelay;
	private int minSpawnDelay;
	private boolean stunned;
	private boolean custom;
	
	public SpawnerStatsStorage() {
		this.maxSpawnDelay = 1200;
		this.minSpawnDelay = 400;
		this.stunned = false;
		this.custom = false;
		
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
	public void setMaxSpawnDelay(int amount) {
		this.maxSpawnDelay = amount;
	}
	
	@Override
	public void setMinSpawnDelay(int amount) {
		this.minSpawnDelay = amount;
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
	public boolean isCustom() {
		return custom;
	}

	@Override
	public void setCustom(boolean bool) {
		custom = bool;
	}

	
}
