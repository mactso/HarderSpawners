package com.mactso.harderspawners.capabilities;

public interface ISpawnerStatsStorage
{
	public int getMinSpawnDelay();
	public int getMaxSpawnDelay();
	public int getDurability();
	public boolean isStunned();
	public boolean isInfiniteDurability();
	public boolean isInitialized();

	
	public void setMinSpawnDelay(int amount);
	public void setMaxSpawnDelay(int amount);
	public void setDurability(int amount);
	public void setStunned(boolean bool);
	public void setInfinite(boolean bool);
	public void setInitialized();

}
