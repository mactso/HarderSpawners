package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.common.logic.SpawnerInitialization;
import com.mactso.harderspawners.common.managers.MobSpawnerManager;
import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * Adapter class to isolate modloader-specific storage logic for
 * SpawnerStatsStorage.
 */
public final class SpawnerStatsAdapter {

	private SpawnerStatsAdapter() {
	} // prevent instantiation

	/**
	 * Returns a wrapper around the stats for the given spawner. Returns null if the
	 * spawner has no valid entity ID or does not implement ISpawnerStats.
	 */
	public static SpawnerStatsWrapper getOrCreateStats(SpawnerBlockEntity sbe) {
		if (!(sbe instanceof ISpawnerStats statsHolder))
			return null;
		SpawnerStatsStorage stats = statsHolder.getSpawnerStatsStorage();

		// Save current spawner NBT
		CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
		String entityId = extractEntityId(spawnerTag);

		if (entityId == null || entityId.isEmpty())
			return null;

	    if (SpawnerUtilityMethods.isTrialSpawner(sbe)) 
	    	return null; // skip trial spawners
		
		// Migration for old spawner storage stats: populate blank entityId
		if (!stats.isInitialized()) {
			if (MyConfig.isDebug()) {
				MyUtilities.debugMsg(1,
						"getOrCreateStats @ " + sbe.getBlockPos() + "\n  initialized=" + stats.isInitialized()
								+ "\n  storedEntityId=" + stats.getOriginalEntityId() + "\n  extractedEntityId="
								+ entityId);
			}
			stats.setOriginalEntityId(entityId);
			// Migration for old spawner storage stats: populate blank entityId
			if (!stats.isInitialized()) {
				if (MyConfig.isDebug())
					MyUtilities.debugMsg(1, "getOrCreateStats: Initializing stats entityId -> " + entityId);
				stats.setOriginalEntityId(entityId);
			}
		}

		return new SpawnerStatsWrapper(sbe, stats, entityId);
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
	 */
	public static class SpawnerStatsWrapper {

		private final SpawnerBlockEntity sbe;
		private final SpawnerStatsStorage stats;
		private final String originalEntityId;

		private SpawnerStatsWrapper(SpawnerBlockEntity sbe, SpawnerStatsStorage stats, String entityId) {
			this.sbe = sbe;
			this.stats = stats;
			this.originalEntityId = entityId;

			if (!stats.isInitialized()) {
				initializeStats();
			}

			// Record position safely
			SpawnerPositionManager.recordSpawnerPos(sbe);
		}

		/** Initialize a new spawner's stats */
		private void initializeStats() {
			if (MyConfig.isDebug())
				MyUtilities.debugMsg(1, "Initializing Spawner");

			CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
			SpawnerInitialization.applyConfigToMonsterSpawners(sbe, spawnerTag);
			if (MyConfig.isDebug())
				MyUtilities.debugMsg(0,
						"PRE-INIT STATS" + "\n  initialized=" + stats.isInitialized() + "\n  stunned="
								+ stats.isStunned() + "\n  infinite=" + stats.isInfinite() + "\n  lifespanTicks="
								+ stats.getLifespan());
			stats.setStunned(false);

			// Lifespan config
			MobSpawnerManager.SpawnerLifespanItem lifespanConfig = MobSpawnerManager
					.getLifespanForMob(originalEntityId);
			stats.setInfinite(lifespanConfig.isInfiniteLifespan());

            // Cache original min/max spawn delays
            stats.setOriginalMinSpawnDelay(spawnerTag.getIntOr("MinSpawnDelay", 200));
            stats.setOriginalMaxSpawnDelay(spawnerTag.getIntOr("MaxSpawnDelay", 800));
			// -------- INPUTS DUMP --------
			if (MyConfig.isDebug())
				MyUtilities.debugMsg(0,
						"INIT INPUTS @ " + sbe.getBlockPos() + "\n  entityId=" + originalEntityId
								+ "\n  lifespanConfig[min=" + lifespanConfig.getMinLifespan() + ", max="
								+ lifespanConfig.getMaxLifespan() + "]" + "\n  originalMinDelay="
								+ stats.getOriginalMinSpawnDelay() + "\n  originalMaxDelay="
								+ stats.getOriginalMaxSpawnDelay());

			if (MyConfig.isDebug())
				MyUtilities.debugMsg(0, "Pre LIFESPAN CALCULATION" + "\n  LifespanValue=" + stats.getLifespan());

			// Initialize lifespan
			if (!stats.isInfinite()) {
				long avgTicks = averageSpawnDelay();
				int lifespanValue = lifespanConfig.initLifespanValue();
				long lifespan = avgTicks * lifespanValue;
				stats.setLifespan(lifespan);
				
				if (MyConfig.isDebug())
					MyUtilities.debugMsg(0, "LIFESPAN CALCULATION" + "\n  avgSpawnDelayTicks=" + avgTicks
							+ "\n  initLifespanValue=" + stats.getLifespan());

			} else {
				stats.setLifespan(Long.MAX_VALUE);
			}

			stats.setInitialized();

			// ---------------- DEBUG INSTRUMENTATION ----------------
			if (MyConfig.isDebug())
				MyUtilities.debugMsg(0,
						"POST-INIT STATS @ " + sbe.getBlockPos() + "\n  initialized=" + stats.isInitialized()
								+ "\n  stunned=" + stats.isStunned() + "\n  infinite=" + stats.isInfinite()
								+ "\n  finalLifeSpanTicks=" + stats.getLifespan());

			sbe.setChanged();
		}

		// --- Public API ---

		public boolean isInfinite() {
			return stats.isInfinite();
		}

		public boolean isStunned() {
			return stats.isStunned();
		}

		public void setStunned(boolean stunned) {
			stats.setStunned(stunned);
			sbe.setChanged();
		}

		public boolean isExpired() {
			return !stats.isInfinite() && stats.getLifespan() <= 0;
		}

		/** Returns average spawn delay from cached min/max */
		public long averageSpawnDelay() {
			return (stats.getOriginalMinSpawnDelay() + stats.getOriginalMaxSpawnDelay()) / 2L;
		}

		/** Remaining estimated spawns */
		public int getEstimatedSpawns() {
			if (stats.isInfinite())
				return -1;
			long avgSpawn = averageSpawnDelay();
			return (int) Math.max(stats.getLifespan() / avgSpawn, 0);
		}

		/** Decrement lifespan by average spawn delay */
		public void decrementLifespan() {
			long avgSpawn = averageSpawnDelay();
			stats.setLifespan(stats.getLifespan() - avgSpawn);
			if (stats.getLifespan() < 0)
				stats.setLifespan(0);;
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
		public long getLifespan() {
			return stats.getLifespan();
		}

		public int getOriginalMinSpawnDelay() {
			return stats.getOriginalMinSpawnDelay();
		}

		public int getOriginalMaxSpawnDelay() {
			return stats.getOriginalMaxSpawnDelay();
		}

		public boolean isInitialized() {
			return stats.isInitialized();
		}

		public String getOriginalEntityId() {
			return originalEntityId;
		}
	}
}
