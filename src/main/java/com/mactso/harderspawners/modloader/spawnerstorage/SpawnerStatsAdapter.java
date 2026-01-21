package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.common.managers.MobSpawnerManager;
import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * Adapter class to isolate NeoForge-specific storage logic.
 * All direct access to SpawnerAttachments or SpawnerStatsStorage goes through this adapter.
 */
public final class SpawnerStatsAdapter {

    private SpawnerStatsAdapter() {} // prevent instantiation

    /**
     * Returns a wrapper around the stats for the given spawner.
     * Populates old spawners with blank entity IDs.
     * Returns null if the spawner has no valid entity ID.
     */
    public static SpawnerStatsWrapper getOrCreateStats(SpawnerBlockEntity sbe) {
        // Quick path: already-initialized stats
        SpawnerStatsStorage existingStats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());
        
        CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
        String entityId = extractEntityId(spawnerTag);
        if (entityId == null || entityId.isEmpty()) return null;
        
        // Stats exist: return their wrapper.
        if (existingStats != null && existingStats.isInitialized()) {
    	
            String originalEntityId = existingStats.getOriginalEntityId();
            // --- Migration for old spawner storage stats: populate blank entityId ---
            if (originalEntityId.isEmpty()) {
                originalEntityId = entityId;
                // Persist the fixed entityId back into storage
                existingStats.setOriginalEntityId(originalEntityId);
                sbe.setChanged(); // ensure persistence
            }
            return new SpawnerStatsWrapper(sbe, originalEntityId);
        }
        // No existing stats: create wrapper from current entity ID
        return new SpawnerStatsWrapper(sbe, entityId);
    }

    /**
     * Extracts the current entity ID from the spawner tag 
     */
    public static String extractEntityId(CompoundTag spawnerTag) {
        CompoundTag spawnData = spawnerTag.getCompound("SpawnData").orElse(null);
        if (spawnData != null && !spawnData.isEmpty()) {
            CompoundTag entityData = spawnData.getCompound("entity").orElse(null);
            if (entityData != null && !entityData.isEmpty()) {
                String id = entityData.getString("id").orElse("").trim();
                if (!id.isEmpty()) return id;
            }
        }
        return null;
    }

    /**
     * Wrapper class exposing public spawner stats logic.
     * Handles lifespan-based expiration, average spawn delay, and original entity tracking.
     */
    public static class SpawnerStatsWrapper {

        private final SpawnerBlockEntity sbe;
        private final SpawnerStatsStorage stats;

        /** Original entity ID at the time of wrapper creation. Immutable. */
        private final String originalEntityId;

        /** Constructor for uninitialized spawner: performs full initialization */
        private SpawnerStatsWrapper(SpawnerBlockEntity sbe, String entityId) {
            this.sbe = sbe;
            
            this.originalEntityId = entityId;

            SpawnerStatsStorage existingStats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());

            if (existingStats == null) {
                this.stats = new SpawnerStatsStorage();
                sbe.setData(SpawnerAttachments.SPAWNER_STATS.get(), stats);
            } else {
                this.stats = existingStats;
            }

            if (!stats.isInitialized()) {
                initializeStats();
            }
            
            // --- Record position safely ---
            SpawnerPositionManager.recordSpawnerPos(sbe);
        }

        /** Initialize a new spawner's stats */
        private void initializeStats() {
        	MyUtilities.debugMsg(0, "Initializing Spawner");
            CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
            SharedUtilityMethods.applyConfigToMonsterSpawners(sbe, spawnerTag);

            stats.setStunned(false);

            // Use original entity ID consistently
            MobSpawnerManager.SpawnerLifespanItem lifespanConfig = MobSpawnerManager.getLifespanForMob(originalEntityId);
            stats.setInfinite(lifespanConfig.isInfiniteLifespan());

            // Cache original min/max spawn delays
            stats.setOriginalMinSpawnDelay(spawnerTag.getIntOr("MinSpawnDelay", 200));
            stats.setOriginalMaxSpawnDelay(spawnerTag.getIntOr("MaxSpawnDelay", 800));

            // Initialize lifespan
            if (!stats.isInfinite()) {
            	MyUtilities.debugMsg(0, "Initializing Lifespan");
                long avgTicks = averageSpawnDelay();
                stats.setLifespan(lifespanConfig.initLifespanValue() * avgTicks);
            } else {
                stats.setLifespan(Long.MAX_VALUE);
            }

            stats.setInitialized();
            sbe.setChanged();
        }

        // --- Public API ---
        public boolean isInfinite() { return stats.isInfinite(); }
        public boolean isStunned() { return stats.isStunned(); }
        public void setStunned(boolean stunned) { stats.setStunned(stunned); sbe.setChanged(); }
        public boolean isExpired() { return stats.hasExpired(); }

        /** Returns average spawn delay from cached min/max */
        public long averageSpawnDelay() {
            return (stats.getOriginalMinSpawnDelay() + stats.getOriginalMaxSpawnDelay()) / 2L;
        }

        /** Remaining estimated spawns */
        public int getEstimatedSpawns() {
            if (stats.isInfinite()) return -1;
            long avgSpawn = averageSpawnDelay();
            return (int) Math.max(stats.getLifespan() / avgSpawn, 0);
        }

        /** Decrement lifespan by average spawn delay */
        public void decrementLifespan() {
            stats.decrementLifespan(averageSpawnDelay());
            sbe.setChanged();
        }

        /** Set lifespan explicitly */
        public void setLifespan(long ticks) {
            stats.setLifespan(ticks);
            sbe.setChanged();
        }
        
		public void setInfinite(boolean b) {
            stats.setInfinite(b);
		}

        // --- Accessors ---
        public long getLifespan() { return stats.getLifespan(); }
        public int getOriginalMinSpawnDelay() { return stats.getOriginalMinSpawnDelay(); }
        public int getOriginalMaxSpawnDelay() { return stats.getOriginalMaxSpawnDelay(); }
        public boolean isInitialized() { return stats.isInitialized(); }

        /** Returns the original entity ID captured when the wrapper was created */
        public String getOriginalEntityId() { return originalEntityId; }


    }
}
