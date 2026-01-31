package com.mactso.harderspawners.common.logic;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.adapter.Adapters;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class ProcessSpawners {

	static final Logger LOGGER = LogManager.getLogger();
	public static final int ABOUT_TO_SPAWN = 1;

	/**
	 * Seek and process nearby spawners within 32 blocks (x,y,z ) of the player.
	 * 
	 * Attach Attachments with mod Spawner Values and Calculated values to each
	 * spawner found Record the spawner blockpos in a list for later use
	 * 
	 * Process each spawner found: Recover from stunned state Check if spawner is
	 * nearing expiration time and make "failing" special effects If spawner has
	 * passed expiration time, then 1) destroy it with no drop 2) based on the
	 * config, optionally explode the spawner.
	 * 
	 * Called by handleBlockPlacement, handleBucketPlacement, onPlayerTick
	 */
	public static void findAndProcessNearbySpawners(ServerPlayer serverPlayer) {
		ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
		BlockPos playerPos = serverPlayer.blockPosition();
		ChunkPos playerChunk = new ChunkPos(playerPos);
		long gameTime = serverLevel.getGameTime();

		int localX = playerPos.getX() & 15;
		int localZ = playerPos.getZ() & 15;
		boolean skipWest = localX < 8, skipEast = localX >= 8, skipNorth = localZ < 8, skipSouth = localZ >= 8;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (shouldSkipChunk(dx, dz, skipWest, skipEast, skipNorth, skipSouth))
					continue;

				LevelChunk chunk = serverLevel.getChunk(playerChunk.x + dx, playerChunk.z + dz);
				ArrayList<BlockEntity> blockEntitiesSnapshot = new ArrayList<BlockEntity>(
						chunk.getBlockEntities().values());

				for (BlockEntity be : blockEntitiesSnapshot) {
					// Skip block entities that got destroyed during this loops
					if (be.isRemoved())
						continue;

					if (!(be instanceof SpawnerBlockEntity sbe))
						continue;
					// Skip spawners that got destroyed during this loops
					if (sbe.isRemoved())
						continue;

					if (isSpawnerTooFarAway(playerPos, sbe.getBlockPos(), 32))
						continue;

					BaseSpawner spawner = sbe.getSpawner();
					if (spawner == null)
						continue;

					CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
					if (MyConfig.isDebug())
						MyUtilities.debugMsg(2, spawnerTag.toString());

					if (SpawnerUtilityMethods.isTrialSpawner(sbe))
						continue; // skip trial spawners

					if (!spawnerTag.isEmpty()) {

						String entityId = SpawnerStatsAdapter.extractEntityId(spawnerTag);
						if (entityId == null)
							continue;
						if (entityId.isEmpty() || entityId.isBlank())
							continue;

						SpawnerPositionManager.recordSpawnerPos(sbe);
						int delay = getSpawnerDelay(sbe, spawner, spawnerTag);

						if (gameTime % 20 == 0) {
							SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
							if (statsWrapper.isStunned()) {
								// this is a hack to avoid client side mixin.
								SpecialEffects.sendSpawnerParticles(serverLevel, sbe.getBlockPos());
							}
						}

						if (gameTime % 200 == 1) { // check every 6 seconds.
							SpawnerLightLogic.removeLightSourcesAboveASpawner(serverLevel, sbe);
						}
						if (delay == ABOUT_TO_SPAWN) {
							// this is not "hot". only once every 200-800 ticks.
							processSpawnerIfReady(serverLevel, sbe, spawner, spawnerTag);
							// Skip spawners that got destroyed in processSpawners
							if (sbe.isRemoved())
								continue;
						}
					}
				}
			}
		}
	}

	private static boolean shouldSkipChunk(int dx, int dz, boolean skipWest, boolean skipEast, boolean skipNorth,
			boolean skipSouth) {
		if (dx < -1 && skipWest)
			return true;
		if (dx > 1 && skipEast)
			return true;
		if (dz < -1 && skipNorth)
			return true;
		if (dz > 1 && skipSouth)
			return true;
		return false;
	}

	// this gets the private spawnDelay counter in BaseSpawner via reflection.
	public static int getSpawnerDelay(SpawnerBlockEntity sbe, BaseSpawner spawner, CompoundTag tag) {
		int delay = -Integer.MAX_VALUE;
		boolean tagSaved = false;

		if (Adapters.isWorking()) {
			delay = Adapters.getSpawnDelay(spawner);
		}

		if (delay == -Integer.MAX_VALUE) {
			tag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
			delay = tag.getInt("Delay").orElse( 0 );
			tagSaved = true;
		}

		if (tagSaved) {
			if (MyConfig.isDebug())
				MyUtilities.debugMsg(2, sbe.getBlockPos(), "Delay fallback applied: " + delay);
		}

		return delay;
	}

	/**
	 * Returns true if the target position is more than maxDistance away on any axis
	 * from the reference position.
	 */
	private static boolean isSpawnerTooFarAway(BlockPos reference, BlockPos target, int maxDistance) {
		int dx = Math.abs(reference.getX() - target.getX());
		if (dx > maxDistance)
			return true;

		int dy = Math.abs(reference.getY() - target.getY());
		if (dy > maxDistance)
			return true;

		int dz = Math.abs(reference.getZ() - target.getZ());
		return dz > maxDistance;
	}

	public static void processSpawnerIfReady(ServerLevel serverLevel, SpawnerBlockEntity sbe, BaseSpawner spawner,
			CompoundTag spawnerTag) {
		if (sbe == null || spawner == null || serverLevel == null)
			return;
		// ignore spawners destroye during this loop.
		if (sbe.isRemoved())
			return;

		SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
		if (statsWrapper == null) // null if spawner lacks an entityId
			return; // can't process a spawner with no stats

		SpawnerPositionManager.recordSpawnerPos(sbe);

		boolean recovered = SpawnerStunLogic.doSpawnerRecoverFromStun(sbe, spawnerTag, statsWrapper);
		if ((MyConfig.isDebug()) && (recovered)) {
			MyUtilities.debugMsg(1, "Spawner recovered from stun.");
		}

		statsWrapper.decrementLifespan();

		SpawnerLifespan.handleSpawnerEndOfLife(serverLevel, sbe, statsWrapper, spawnerTag);
		// If the spawner was destroyed mid-tick or inside handleSpawnerEndOfLife, stop
		// processing
		if (sbe.isRemoved())
			return;

		// --- Additional "monster only"spawner effects ---
		if (SpawnerUtilityMethods.isMonsterSpawner(sbe, spawnerTag)) {
			SpawnerLightLogic.reapplyCustomLightRules(sbe, spawnerTag, statsWrapper);
			SpawnerLightLogic.destroyLightingNearSpawner(sbe);
			SpecialEffects.doSpawnerExpiringSoonEffects(sbe);

		}
	}

}
