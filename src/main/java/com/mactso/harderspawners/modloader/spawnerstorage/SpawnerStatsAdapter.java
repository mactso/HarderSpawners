package com.mactso.harderspawners.modloader.spawnerstorage;

import java.util.Optional;

import com.mactso.harderspawners.common.logic.ProcessSpawners;
import com.mactso.harderspawners.common.managers.MobSpawnerManager;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Adapter class to isolate NeoForge-specific storage logic.
 * All direct access to SpawnerAttachments or SpawnerStatsStorage should
 * go through this adapter.
 */
public final class SpawnerStatsAdapter {

	private static final org.slf4j.Logger LOGGERUTIL =  LogUtils.getLogger();
	
    private SpawnerStatsAdapter() {} // prevent instantiation

    /**
     * Returns a wrapper around the stats for the given spawner.
     * If the spawner has no entityId, returns null.
     * If the stats don't exist yet, creates and initializes them.
     */
    public static SpawnerStatsWrapper getOrCreateStats(SpawnerBlockEntity sbe) {

        // Quick path: already-initialized stats
        SpawnerStatsStorage existingStats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());
        if (existingStats != null && existingStats.isInitialized()) {
            return new SpawnerStatsWrapper(existingStats, sbe, false); // no init, just wrap
        }

        // Do not create stats or statswrapper if entity id is empty or null.
        CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
		
        // Safely get nested SpawnData -> entity -> id
        Optional<CompoundTag> optSpawnData = spawnerTag.getCompound("SpawnData");
        Optional<CompoundTag> optEntityData = optSpawnData.flatMap(spawnData -> spawnData.getCompound("entity"));
        Optional<String> optEntityId = optEntityData.flatMap(entityData -> entityData.getString("id"));

        // Return null if missing or empty
        if (optEntityId.isEmpty() || optEntityId.get().isEmpty()) {
            return null;
        }

        // Full wrapper: will initialize stats
        return new SpawnerStatsWrapper(sbe, optEntityId.get());
    }

    /**
     * Wrapper class that exposes only the public interface for logic code.
     * Tracks whether this wrapper/stats was just created for the first time.
     */
    public static class SpawnerStatsWrapper {
        private final SpawnerBlockEntity sbe;
        private final SpawnerStatsStorage stats;
        private final String entityId;
        private final boolean justCreated;

        /** Constructor for already-initialized stats */
        private SpawnerStatsWrapper(SpawnerStatsStorage stats, SpawnerBlockEntity sbe, boolean justCreated) {
            this.sbe = sbe;
            this.stats = stats;
            this.entityId = null; // already initialized, entityId not needed
            this.justCreated = justCreated;
        }

        /** Constructor for uninitialized spawner: will perform full initialization */
        private SpawnerStatsWrapper(SpawnerBlockEntity sbe, String entityId) {
            this.sbe = sbe;
            this.entityId = entityId;

            SpawnerStatsStorage existingStats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());
            this.justCreated = (existingStats == null || !existingStats.isInitialized());

            if (existingStats == null) {
                this.stats = new SpawnerStatsStorage();
                sbe.setData(SpawnerAttachments.SPAWNER_STATS.get(), stats);
            } else {
                this.stats = existingStats;
            }

            if (!stats.isInitialized()) {
                initializeStats();
            }
        }

        /** Initialize a new spawner's stats and normalize its tag */
        private void initializeStats() {
            // 1. Snapshot vanilla spawner
			CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
            // 2. Normalize spawner config (ONE TIME)
            doApplyConfigToMonsterSpawners(sbe, spawnerTag);

            // 3. Backup normalized baseline
            stats.backupOriginalSpawner(sbe);
            stats.setStunned(false);

            MobSpawnerManager.SpawnerDurabilityItem durabilityConfig =
                    MobSpawnerManager.getDurabilityForMob(entityId);

            stats.setDurability(durabilityConfig.initDurabilityValue());
            stats.setInfinite(durabilityConfig.isInfiniteDurability());

            ChunkAccess chunk = sbe.getLevel().getChunk(sbe.getBlockPos());
            stats.setSpawnerExpirationTime(stats.getInitialFailureTime(chunk.getInhabitedTime()));

            stats.setInitialized();
            sbe.setChanged();
        }

        /** Returns true if this wrapper/stats was created during this call */
        public boolean isJustCreated() {
            return justCreated;
        }

        // --- Public API ---
        public boolean isInfinite() { return stats.isInfinite(); }
        public boolean isStunned() { return stats.isStunned(); }
        public void setStunned(boolean stunned) { stats.setStunned(stunned); sbe.setChanged(); }
        public int getDurability() { return stats.getDurability(); }
        public void setDurability(int durability) { stats.setDurability(durability); sbe.setChanged(); }
        public boolean hasExpired() { return stats.hasExpired(sbe); }
        public long getAverageTimePerSpawn() { return stats.getAverageTimePerSpawn(); }
        public long getSpawnerExpirationTime() { return stats.getSpawnerExpirationTime(); }
        public CompoundTag getOriginalTag() { return stats.getOriginalTag(); }
        public boolean isInitialized() { return stats.isInitialized(); }
		public void setSpawnerExpirationTime(long newExpirationTime) {
			stats.setSpawnerExpirationTime(newExpirationTime);
		}
    }

    public static void doApplyConfigToMonsterSpawners(SpawnerBlockEntity sbe, CompoundTag tag) {
    	
        CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
        
        Optional<CompoundTag> optTag = spawnerTag.getCompound("SpawnData");
        if (optTag.isEmpty())
        	return;
        CompoundTag spawnDataTag = optTag.get();
        if (spawnDataTag.isEmpty()) return;
 
        if (ProcessSpawners.isMonsterSpawner(sbe, spawnerTag)) {
            // MaxNearbyEntities
            Optional<Integer> optMaxNearby = tag.getInt("MaxNearbyEntities");
            if (optMaxNearby.isEmpty() ||
                optMaxNearby.get() != MyConfig.getMaxNearbyEntities()) {
                tag.putInt("MaxNearbyEntities", MyConfig.getMaxNearbyEntities());
            }
            // RequiredPlayerRange
            Optional<Integer> optRequiredRange = tag.getInt("RequiredPlayerRange");
            if (optRequiredRange.isEmpty() ||
                optRequiredRange.get() != MyConfig.getRequiredPlayerRange()) {
                tag.putInt("RequiredPlayerRange", MyConfig.getRequiredPlayerRange());
            }

            // SpawnRange
            Optional<Integer> optSpawnRange = tag.getInt("SpawnRange");
            if (optSpawnRange.isEmpty() ||
                optSpawnRange.get() != MyConfig.getSpawnRange()) {
                tag.putInt("SpawnRange", MyConfig.getSpawnRange());
            }
            
            

            Optional<Tag> workSpawnData = ProcessSpawners.buildCustomLightLevelSpawnData(spawnDataTag);
            if (workSpawnData.isPresent() && !spawnDataTag.equals(workSpawnData.get())) {
                tag.put("SpawnData", workSpawnData.get());
            }

            // Save tag back to spawner
			ScopedCollector loadPreport = new ScopedCollector((org.slf4j.Logger) LOGGERUTIL);
			sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), TagValueInput.create(loadPreport, sbe.getLevel().registryAccess(), spawnerTag));
			SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);


        }
    }
}
