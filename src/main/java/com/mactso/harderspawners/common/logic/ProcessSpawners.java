package com.mactso.harderspawners.common.logic;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.adapter.Adapters;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.config.MyConfig.EndOfLifespanAction;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class ProcessSpawners {

	private static final Logger LOGGER = LogManager.getLogger();
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
		ServerLevel serverLevel = serverPlayer.level();
		BlockPos playerPos = serverPlayer.blockPosition();
		ChunkPos playerChunk = new ChunkPos(playerPos);

		int localX = playerPos.getX() & 15;
		int localZ = playerPos.getZ() & 15;
		boolean skipWest = localX < 8, skipEast = localX >= 8, skipNorth = localZ < 8, skipSouth = localZ >= 8;

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (shouldSkipChunk(dx, dz, skipWest, skipEast, skipNorth, skipSouth))
					continue;

				LevelChunk chunk = serverLevel.getChunk(playerChunk.x + dx, playerChunk.z + dz);
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof SpawnerBlockEntity sbe))
						continue;
					if (isSpawnerTooFarAway(playerPos, sbe.getBlockPos(), 32))
						continue;
					BaseSpawner spawner = sbe.getSpawner();
					if ( spawner == null )
						continue;
					
					SpawnerPositionManager.recordSpawnerPos(sbe);
					CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);

					if (!spawnerTag.isEmpty()) {
						String entityId = SpawnerStatsAdapter.extractEntityId(spawnerTag);
						if (entityId == null) continue;
						if (entityId.isEmpty() || entityId.isBlank()) continue;

						int delay = getSpawnerDelay(sbe, spawner, spawnerTag);
						if (delay == ABOUT_TO_SPAWN) {
							// this is not "hot".  only once every 200-800 ticks.
							processSpawnerIfReady(serverLevel, sbe, spawner, spawnerTag); 

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
			delay = Adapters.getDelay(spawner);
		}

		if (delay == -Integer.MAX_VALUE) {
			tag = SharedUtilityMethods.saveSpawnerToTag(sbe);
			delay = tag.getShort("Delay").orElse((short) 0);
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


		SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
		if (statsWrapper == null) // null if spawner lacks an entityId
			return; // can't process a spawner with no stats

		SpawnerPositionManager.recordSpawnerPos(sbe);
		
		boolean recovered = SpawnerStunLogic.doSpawnerRecoverFromStun(sbe, spawnerTag, statsWrapper);
		if ((MyConfig.isDebug()) && (recovered)) {
			MyUtilities.debugMsg(1, "Spawner recovered from stun.");
		}

		statsWrapper.decrementLifespan();
//		SharedUtilityMethods.logSpawnerState(1, "handleSpawnerLifespanEnd.pre", sbe, statsWrapper);

		if (handleSpawnerLifespanEnd(serverLevel, sbe, statsWrapper, spawnerTag)) {
			return; // Spawner was destroyed or exploded
		}

		// --- Additional "monster only"spawner effects ---
		if (SharedUtilityMethods.isMonsterSpawner(sbe, spawnerTag)) {
//	TODO:		reapplyCustomLightRules(sbe, spawnerTag, statsWrapper);
			SharedUtilityMethods.destroyLightingNearSpawner(sbe);
			SpecialEffects.doSpawnerExpiringSoonEffects(sbe);

		}
	}

	// TODO: Test this every release.
	/**
	 * Handles final cleanup when a spawner's lifespan is exhausted. Removes
	 * extralifespan Displays and destroys the spawner block then Skips explosions
	 * for silverfish spawners to protect End Portals. otherwise checks chance for
	 * explosion.
	 * 
	 * @return true if the spawner expired and was destroyed (with optional
	 *         explosion), false if the spawner is still active.
	 */

	public static boolean handleSpawnerLifespanEnd(ServerLevel serverLevel, SpawnerBlockEntity sbe,
			SpawnerStatsWrapper statsWrapper, CompoundTag spawnerTag) {

		if (serverLevel == null || sbe == null || statsWrapper == null)
			return false;

		boolean spawnerIsExpired = statsWrapper.isExpired();
		// spawnerIsExpired = true; // TODO: debugging statement.
		if (!(spawnerIsExpired)) // spawner within lifespan still
			return false;

		Enum<EndOfLifespanAction> action = MyConfig.getEndOfLifespanAction();
		if (action == MyConfig.EndOfLifespanAction.LINGER) {
			// stun lingering spawner for 25 minutes.
			SpawnerStunLogic.lingerStunSpawner(serverLevel, sbe, statsWrapper, spawnerTag);
			return false;
		}

		// Remove extraLifespan display items near the spawner
		ExtraLifetimeItemDisplays.removeDisplay(serverLevel, sbe);

		// Destroy the spawner block
		BlockPos pos = sbe.getBlockPos();
		SpawnerPositionManager.forgetSpawner(serverLevel, pos);
		serverLevel.destroyBlock(pos, true); // drops loot table drops, not spawner blocks even with silk touch.

		// Avoid Exploding SilverFish Spawners to protect End Portals.
		String entityId = statsWrapper.getOriginalEntityId();
		if ("minecraft:silverfish".equals(entityId)) { // implied null entityId protection.
			return true;
		}

		// Random chance for explosion
		RandomSource chance = serverLevel.getRandom();
		double explodeRoll = 100.0 * chance.nextDouble();
		if (explodeRoll < MyConfig.getSpawnersExplodePercentage()) {
			Vec3 v = new Vec3(pos.getX(), pos.getY(), pos.getZ());
			serverLevel.explode(null, // no entity responsible
					null, // no damage source
					null, // no context
					v.x, v.y, v.z, 4.0f, // explosion strength
					true, // causes block damage
					ExplosionInteraction.BLOCK);
		}
		return true;

	}

	/**
	 * Reapplies configured spawn light-level rules to a spawner. Used defensively
	 * in case other mods modify SpawnData light limits. Rebuilds SpawnData via
	 * codec to preserve entity and equipment data. Reloads the updated tag into
	 * BaseSpawner immediately before spawn. No-op if SpawnData or entity data is
	 * missing.
	 */
	public static void reapplyCustomLightRules(SpawnerBlockEntity sbe, CompoundTag spawnerTag,
			SpawnerStatsWrapper statsWrapper) {

		Optional<CompoundTag> optTag = spawnerTag.getCompound("SpawnData");
		if (optTag.isEmpty())  // overly defensive code. if we are here, it's not empty.
			return;
		CompoundTag spawnDataTag = optTag.get();
		if (spawnDataTag.isEmpty()) // overly defensive code. if we are here, it's not empty.
			return;

		// Use your existing codec-based method to build the new tag
		Optional<Tag> workSpawnData = buildCustomLightLevelSpawnData(spawnDataTag);
		if (workSpawnData == null)
			return;
		if (workSpawnData.isPresent()) {
			spawnerTag.put("SpawnData", workSpawnData.get());
			// Load the modified tag back into the internal BaseSpawner logic
			SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);

			sbe.setChanged();
			MyUtilities.debugMsg(2, "JIT: Light levels reapplied to " + sbe.getBlockPos().toShortString());
		}

	}

	/**
	 * Rebuilds SpawnData with custom light-level spawn rules applied. Preserves
	 * existing entity data and equipment. Uses config-defined hostile spawner light
	 * level. Returns encoded SpawnData tag if entity data exists. Returns
	 * empty/absent result if SpawnData is invalid or missing entity.
	 */
	public static Optional<Tag> buildCustomLightLevelSpawnData(CompoundTag spawnDataTag) {

		SpawnData spawndata = SpawnData.CODEC.parse(NbtOps.INSTANCE, spawnDataTag)
				.resultOrPartial(p_186391_ -> LOGGER.warn("Invalid SpawnData: {}", p_186391_))
				.orElseGet(SpawnData::new);
		Optional<EquipmentTable> equipment = spawndata.equipment();

		int lightLevel = MyConfig.getHostileSpawnerLightLevel();
		int blocklight = lightLevel;
		int skylight = lightLevel;
		CustomSpawnRules c = new SpawnData.CustomSpawnRules(new InclusiveRange<Integer>(0, blocklight),
				new InclusiveRange<Integer>(0, skylight));

		Optional<CompoundTag> optEntityTag = spawnDataTag.getCompound("entity");
		if (optEntityTag.isEmpty())
			return null;
		CompoundTag entityTag = optEntityTag.get();
		SpawnData s = new SpawnData(entityTag, Optional.of(c), equipment);

		return SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, s).result();

	}

}
