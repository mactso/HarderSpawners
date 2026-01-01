package com.mactso.harderspawners.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class SpawnerStatsStorageProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    private final ISpawnerStatsStorage storage = new SpawnerStatsStorage();
    private final LazyOptional<ISpawnerStatsStorage> optional = LazyOptional.of(() -> storage);

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilitySpawner.SPAWNER_STORAGE) {
            return (LazyOptional<T>) optional;
        }
        return LazyOptional.empty();
    }


    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("maxSpawnDelay", storage.getMaxSpawnDelay());
        tag.putInt("minSpawnDelay", storage.getMinSpawnDelay());
        tag.putBoolean("stunned", storage.isStunned());
        tag.putInt("spawnscount", storage.getDurability());
        return tag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registryAccess, CompoundTag nbt) {
        storage.setMaxSpawnDelay(nbt.getIntOr("maxSpawnDelay", 200));
        storage.setMinSpawnDelay(nbt.getIntOr("minSpawnDelay", 800));
        storage.setStunned(nbt.getBooleanOr("stunned", false));
        storage.setDurability(nbt.getIntOr("spawnscount", 100));
    }

    /** Provides access for external invalidation */
    public LazyOptional<ISpawnerStatsStorage> getOptional() {
        return optional;
    }
}
