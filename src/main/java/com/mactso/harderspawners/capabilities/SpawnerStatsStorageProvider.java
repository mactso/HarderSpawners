package com.mactso.harderspawners.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class SpawnerStatsStorageProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag>
{
	ISpawnerStatsStorage storage;

	public SpawnerStatsStorageProvider() {
		storage = new SpawnerStatsStorage();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilitySpawner.SPAWNER_STORAGE)
			return (LazyOptional<T>) LazyOptional.of(() -> storage);
		return LazyOptional.empty();
	}
	
	
	@Override
	public CompoundTag serializeNBT(Provider registryAccess) {
		CompoundTag ret = new CompoundTag();
		ret.putInt("maxSpawnDelay", storage.getMaxSpawnDelay());
		ret.putInt("minSpawnDelay", storage.getMinSpawnDelay());
		ret.putBoolean("stunned", storage.isStunned());
		ret.putInt("spawnscount", storage.getDurability());
		return ret;
	}
	
	
	@Override
	public void deserializeNBT(Provider registryAccess, CompoundTag nbt) {
		int maxSpawnDelay = nbt.getInt("maxSpawnDelay");
		int minSpawnDelay = nbt.getInt("minSpawnDelay");
		boolean stunned = nbt.getBoolean("stunned");
		int spawnscount = nbt.getInt("spawnscount");
		storage.setMaxSpawnDelay(maxSpawnDelay);
		storage.setMinSpawnDelay(minSpawnDelay);
		storage.setStunned(stunned);
		storage.setDurability(spawnscount);
	}
	
}
