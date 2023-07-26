package com.mactso.harderspawners.capabilities;

public interface ISpawnerStatsStorage
{
	public int getMinSpawnDelay();
	public int getMaxSpawnDelay();
	public boolean isStunned();
	public boolean isCustom();

	
	public void setMinSpawnDelay(int amount);
	public void setMaxSpawnDelay(int amount);
	public void setStunned(boolean bool);
	public void setCustom(boolean bool);

}
