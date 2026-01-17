package com.mactso.harderspawners.common.logic;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.adapter.Adapters;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class ProcessSpawners {

	private static final Logger LOGGER = LogManager.getLogger();
	// private static int spam = 0;
	// Could be in your utility class or main mod class
	private static final Map<ServerLevel, Set<BlockPos>> pendingLavaBlocks = new ConcurrentHashMap<>();

//	private static final java.lang.reflect.Field SPAWN_DELAY_FIELD = 
//		    net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
//		        net.minecraft.world.level.BaseSpawner.class, "spawnDelay"
//		    );
	private static int last_delay = Integer.MAX_VALUE;
	
	/**
	 * Adds a lava block to the pending queue to be processed on the next player
	 * tick.
	 */
	public static void queuePendingLava(ServerLevel level, BlockPos pos) {
		pendingLavaBlocks.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(pos);
	}

	/**
	 * Clears all queued pending lava blocks for the given player level. Should be
	 * called once per player tick.
	 *
	 * @param sp The server player whose level will be processed.
	 */
	public static void clearPendingLava(ServerPlayer sp) {

		ServerLevel serverLevel = sp.level();
		// Get the pending lava set for this level
		Set<BlockPos> pending = pendingLavaBlocks.get(serverLevel);
		if (pending == null || pending.isEmpty()) {
			return; // Nothing to do
		}

		MyUtilities.debugMsg(1, "Clearing Lava");
		for (BlockPos pos : pending) {
			BlockState state = serverLevel.getBlockState(pos);
			if (state.getBlock() == Blocks.LAVA) {
				// Remove the lava block (flash was displayed)
				serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}

		// Clear the set so we don't process the same blocks again
		pending.clear();
	}

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

		// debug sounds here.  plays once per 6 seconds.
//		spam = (spam + 1) % 120;
//		if (spam == 0) {
//			level.playSound(null, serverPlayer.blockPosition(), ModSounds.SPAWNER_RECOVERS.value(), SoundSource.BLOCKS,
//					1.0f, 1.0f);
//			MyUtilities.debugMsg(0, "play sound");
//		}

		// Determine which far chunks we can skip based on player's position in the
		// chunk
		int localX = playerPos.getX() & 15; // 0-15 in current chunk
		int localZ = playerPos.getZ() & 15;

		// we only care about spawners 32 blocks away so optimize the chunk scan.
		boolean skipWest = localX < 8; // player is on east side, can skip far west chunks
		boolean skipEast = localX >= 8; // player is on west side, can skip far east chunks
		boolean skipNorth = localZ < 8; // player is on south side, can skip far north chunks
		boolean skipSouth = localZ >= 8; // player is on north side, can skip far south chunks

		// Scan nearby chunks
		for (int dx = -2; dx <= 2; dx++) {
			if (dx < -1 && skipWest)
				continue; // skip far west
			if (dx > 1 && skipEast)
				continue; // skip far east
			for (int dz = -2; dz <= 2; dz++) {
				if (dz < -1 && skipNorth)
					continue; // skip far north
				if (dz > 1 && skipSouth)
					continue; // skip far south

				ChunkPos chunkPos = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
				LevelChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);

				// Iterate all block entities in the chunk
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof SpawnerBlockEntity sbe))
						continue;

					// Axis-aligned 32-block x,y,z distance check
					if (isSpawnerTooFarAway(playerPos, sbe.getBlockPos(), 32))
						continue;

					// Initialize stats (may return null if no entity)
					SpawnerStatsAdapter.SpawnerStatsWrapper stats = SpawnerStatsHelper.getOrCreateStats(sbe);
					if (stats == null)
						continue;

					SpawnerRegistry.recordSpawnerPos(sbe);
					BaseSpawner spawner = sbe.getSpawner();
					CompoundTag spawnerTag = new CompoundTag();

					// Check spawner delay
					// optimize this later with an access widener or Reflection to
					// BaseSpawner.spawnDelay
					MyUtilities.debugMsg(1, "Spawner.spawnerDelay Adapter check ");
					int delay = -Integer.MAX_VALUE;
					boolean tagSaved = false;
					if (Adapters.isWorking()) {
						delay = Adapters.getDelay(spawner);
						MyUtilities.debugMsg(2, "Adapter working.  Delay = "+ delay);
					} 

					if (delay == -Integer.MAX_VALUE){
						spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
						Optional<Short> optDelay = spawnerTag.getShort("Delay");
						delay = optDelay.orElse((short) 0); // provide a default if missing
						tagSaved = true;
						MyUtilities.debugMsg(2, "Adapter failed.  Delay = "+ delay);
					}

					if (last_delay < delay) 
						MyUtilities.debugMsg(1, "Post Spawn Delay = "+ delay);
						
					last_delay=delay;
					
					if (delay == 1) {
						if (!tagSaved) {
							spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
							tagSaved = true;
						}
						MyUtilities.debugMsg(2, sbe.getBlockPos(), "Delay: " + delay);
						doProcessSpawner(serverLevel, sbe, spawner, spawnerTag);
					}
				}
			}
		}
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

	public static void doProcessSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe, BaseSpawner spawner,
			CompoundTag spawnerTag) {
		if (sbe == null || spawner == null || serverLevel == null)
			return;

		SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);

		// Handle stunned spawner logic (reset delays, etc.)
		boolean changed = SpawnerStunLogic.doSpawnerRecoverFromStun(sbe, spawnerTag, statsWrapper);
		if (changed)
			MyUtilities.debugMsg(1, "Spawner recovered from stun.");

		// If this is a monster spawner, destroy nearby lights, and check failure
		if (ProcessSpawners.isMonsterSpawner(sbe, spawnerTag)) {
			
			// Re-apply custom light rules ONLY right before the spawn attempt since other mods are removing them.
	        reapplyCustomLightRules(sbe, spawnerTag);
	        
			SharedUtilityMethods.doDestroyLightingNearSpawner(sbe);
			SpecialEffects.doSpawnerExpiringSoonEffects(sbe);
			ProcessSpawners.doSpawnerExpires(serverLevel, sbe);
		}
	}

	// TODO: Test this every release.
	// cap is confirmed valid before this method is called.
	// If a spawner has reached or exceeded its lifespan, it expires. // if they are
	// expired, handle the expiration by poofing or exploding
	// this routine cleans up and then either poofs the spawner or explodes the
	// spawner.

	public static void doSpawnerExpires(ServerLevel serverLevel, SpawnerBlockEntity sbe) {

		// Wrap the stats
		SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);

		if (statsWrapper == null) // empty spawncage lacking entity
			return;

		boolean spawnerHasExpired = statsWrapper.hasExpired();
		// spawnerHasExpired = true; // TODO: debugging statement.
		if (!(spawnerHasExpired)) // spawner within lifespan still
			return;

		// Remove repair display items near the spawner
		TimeExtensionItemDisplays.removeDisplay(serverLevel, sbe);

		// Destroy the spawner block
		BlockPos pos = sbe.getBlockPos();
		serverLevel.destroyBlock(pos, false);

		// Avoid Exploding SilverFish Spawners to protect End Portals.
		Optional<String> optEntityId = getEntityId(statsWrapper);
		if (optEntityId.isPresent() && optEntityId.get().equals("minecraft:silverfish")) {
		    return;
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
	}
	
	
	/**
	 * Returns the entity ID string from a spawner's original tag, if present.
	 */
	public static Optional<String> getEntityId(SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper) {
	    return statsWrapper.getOriginalTag()
	        .getCompound("SpawnData")
	        .flatMap(spawnData -> spawnData.getCompound("entity"))
	        .flatMap(entityData -> entityData.getString("id"));
	}
	
	
	public static void reapplyCustomLightRules(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {
		Optional<CompoundTag> optTag = spawnerTag.getCompound("SpawnData");
		if (optTag.isEmpty())
			return;
	    CompoundTag spawnDataTag = optTag.get();
	    if (spawnDataTag.isEmpty()) 
	    	return;

	    // Use your existing codec-based method to build the new tag
	    Optional<Tag> workSpawnData = ProcessSpawners.buildCustomLightLevelSpawnData(spawnDataTag);
	    if (workSpawnData == null)
	    	return;
	    if (workSpawnData.isPresent()) {
	        spawnerTag.put("SpawnData", workSpawnData.get());
	        // Crucial: Load the modified tag back into the internal BaseSpawner logic
			SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
			
			sbe.setChanged();
	        MyUtilities.debugMsg(2, "JIT: Light levels reapplied to " + sbe.getBlockPos().toShortString());
	    }
	}

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

	public static boolean isMonsterSpawner(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

		
	    if (spawnerTag == null) {
	        return false;
	    }
	    // --- OLD STYLE ---
	    /*
	    Optional<CompoundTag> optSpawnData = spawnerTag.getCompound("SpawnData");
	    if (optSpawnData.isEmpty()) 
	        return false;
	    CompoundTag spawnDataTag = optSpawnData.get();
	    Optional<CompoundTag> optEntityData = spawnDataTag.getCompound("entity");
	    if (optEntityData.isEmpty())
	        return false;
	    Optional<String> optIdString = optEntityData.get().getString("id");
	    if (optIdString.isEmpty())
	        return false;		
	    Optional<EntityType<?>> eType = EntityType.byString(optIdString.get());
	    if (eType.isEmpty())
	        return false;

	    if (eType.get().getCategory() == MobCategory.MONSTER)
	        return true;
	    */

	    // --- MODERN STYLE (with old style return for easier debugging)  ---
	    /**
	     * flatMap takes a value inside an Optional and applies a function that also returns an Optional.
	     * If the original Optional is empty, it does nothing and returns empty.
	     * It lets you chain multiple Optional-returning operations safely without nested Optionals.
	     */
	    
	    boolean returnVal = spawnerTag.getCompound("SpawnData")
	        .flatMap(spawnData -> spawnData.getCompound("entity"))
	        .flatMap(entityData -> entityData.getString("id"))
	        .flatMap(EntityType::byString)
	        .map(eType -> eType.getCategory() == MobCategory.MONSTER)
	        .orElse(false);
	    
	    return returnVal;
	}

}
