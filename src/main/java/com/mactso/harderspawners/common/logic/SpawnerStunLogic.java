package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implements spawner "stun" behavior by converting spawner destruction into
 * temporary reuse delays.
 *
 * In HarderSpawners, a stunned spawner is not broken. Instead, its spawn timing
 * values are overridden to defer the next spawn for a configurable duration.
 * Once the stun expires, the original spawn delays are restored and the spawner
 * resumes normal operation.
 *
 * This logic supports multiple stun sources (e.g., player break attempts and
 * lifespan exhaustion) while sharing the same recovery mechanism.
 */

public class SpawnerStunLogic {
	// Use SKIP_DELAY_UPDATE to indicate that Delay should not be set (currently: in linger case).
	private static final int SKIP_DELAY_UPDATE = Integer.MIN_VALUE;
	private static final int LINGER_STUN_TICKS = 25 * 60 * 20; // 25 minutes in ticks

	/**
	 * Stuns a spawner when an entity with a tool "break-stuns" a spawner.
	 * Spawner is stunned for the configured 1 to 27 minutes.
	 * Expensive but infrequent activity: reloads spawner NBT.
	 */
	public static void stunSpawnerWithTool(ServerLevel serverLevel, SpawnerBlockEntity sbe, SpawnerStatsWrapper wrapper,
			CompoundTag spawnerTag) {

		MyUtilities.debugMsg(1, sbe.getBlockPos(), "Player with Tool Break-Stuns Spawner");
		int stunnedTicks = MyConfig.getSpawnerTicksStunned();
		stunCoreLogic(serverLevel, sbe, wrapper, spawnerTag, stunnedTicks, stunnedTicks + 10, stunnedTicks + 5);

	}

	/*
	 * Stuns a spawner when it reaches it's end of life and the configured EndOfLife action is "Linger". 
	 * Spawner is stunned for 25 minutes.
	 */ 

	public static void stunSpawnerWithLinger(ServerLevel serverLevel, SpawnerBlockEntity sbe,
			SpawnerStatsWrapper wrapper, CompoundTag spawnerTag) {

		MyUtilities.debugMsg(1, sbe.getBlockPos(), "Spawner end of life and (linger = true) stuns Spawner");
		int stunnedTicks = LINGER_STUN_TICKS;
		stunCoreLogic(serverLevel, sbe, wrapper, spawnerTag, stunnedTicks, stunnedTicks + 10, SKIP_DELAY_UPDATE);

	}

	/*
	 * Core logic for setting a spawner stunned
	 */
	private static void stunCoreLogic(ServerLevel serverLevel, SpawnerBlockEntity sbe, SpawnerStatsWrapper wrapper,
			CompoundTag spawnerTag, int newMinDelay, int newMaxDelay, int newDelay) {
		wrapper.setStunned(true);
		serverLevel.playSound(null, sbe.getBlockPos(), SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);
		spawnerTag.putInt("MinSpawnDelay", newMinDelay);
		spawnerTag.putInt("MaxSpawnDelay", newMaxDelay);
		// Ensure Delay is strictly greater than MinSpawnDelay to avoid logic glitches
		if ((newDelay != SKIP_DELAY_UPDATE) && (newDelay > newMinDelay))
			spawnerTag.putInt("Delay", newDelay);
		SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
		sbe.setChanged();

	}

	/**
	 * Debug print for stunned spawner state vs original tag.
	 */
	public static void doStunDebugMsg(BlockEntity sbe, CompoundTag tag,
			SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper) {
		if (MyConfig.getDebugLevel() <= 0)
			return;

		BlockPos pos = sbe.getBlockPos();
		MyUtilities.debugMsg(1, pos, "Restoring Stunned Spawner");

		int savedMin = statsWrapper.getOriginalMinSpawnDelay();
		int savedMax = statsWrapper.getOriginalMaxSpawnDelay();

		MyUtilities.debugMsg(2, pos, "Stunned Spawner tag values: (min):" + tag.getInt("MinSpawnDelay") + " (max):"
				+ tag.getInt("MaxSpawnDelay"));
		MyUtilities.debugMsg(2, pos, "Restoring Spawner original values: (min):" + savedMin + " (max):" + savedMax);
	}
	
	/**
	 * Core logic for handling spawner break attempts by non-creative players. Stuns
	 * spawners if the feature is enabled or plays failure sound if already stunned.
	 *
	 * @param sp          the server player attempting to break the block
	 * @param serverLevel the level containing the block
	 * @param pos         the position of the block being broken
	 * @param be          the block entity at the position
	 * @param targetBlock the block type at the position
	 * @return true if the event should be canceled
	 */
	public static boolean processSpawnerStun(ServerPlayer sp, ServerLevel serverLevel, SpawnerBlockEntity sbe) {

		SpawnerStatsAdapter.SpawnerStatsWrapper wrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
		if (wrapper == null)
			return false;

		BlockPos pos = sbe.getBlockPos();
		if (wrapper.isStunned()) {
			serverLevel.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.AMBIENT, 1.0f, 1.0f);
			return true;
		}
		MyUtilities.debugMsg(1, pos, "About to stun spawner");
		CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
		stunSpawnerWithTool(serverLevel, sbe, wrapper, spawnerTag);

		MyUtilities.debugMsg(1, pos, "set changed and send block updated");

		sbe.setChanged();
		// Get the block state at the spawner's position
		BlockState state = serverLevel.getBlockState(sbe.getBlockPos());
		// Notify the client that the block and its block entity changed
		serverLevel.sendBlockUpdated(sbe.getBlockPos(), state, state, 3);

		return true;
	}
	/**
	 * Restores spawner from stunned state: resets min and max spawndelays and
	 * resets 'stunned' flag. this is called just before a stunned spawner spawns
	 * again.
	 */
	public static boolean doSpawnerRecoverFromStun(SpawnerBlockEntity sbe, CompoundTag spawnerTag,
			SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper) {

		MyUtilities.debugMsg(1, "running doSpawnerRecoverFromStun\n");
		if (!(sbe.getLevel() instanceof ServerLevel serverLevel))
			return false;

		if (!statsWrapper.isStunned())
			return false;
		
		doStunDebugMsg(sbe, spawnerTag, statsWrapper);

		// Restore original min/max spawn delays from wrapper cache
		int originalMin = statsWrapper.getOriginalMinSpawnDelay();
		int originalMax = statsWrapper.getOriginalMaxSpawnDelay();

		spawnerTag.putInt("MinSpawnDelay", originalMin);
		spawnerTag.putInt("MaxSpawnDelay", originalMax);

		// Load the restored tag into the spawner
		SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
		sbe.setChanged();
		serverLevel.sendBlockUpdated(
			    sbe.getBlockPos(),
			    sbe.getBlockState(),
			    sbe.getBlockState(),
			    3 // flags: 3 = send to clients + render update
			);
		sbe.getLevel().playSound(null, sbe.getBlockPos(), ModSounds.SPAWNER_RECOVERS, SoundSource.BLOCKS, 1.0f, 1.0f);

		statsWrapper.setStunned(false);

		return true;
	}


	/*
	 * If passed a stunned spawner, this Restores baseline spawner values and calculates a new spawn delay
	 * this is called when a stunner is repaired.
	 */
	public static void restoreStunnedSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe,
			SpawnerStatsWrapper statsWrapper) {
	
		if (!statsWrapper.isStunned())
			return;
	
		int minSpawnDelay = statsWrapper.getOriginalMinSpawnDelay();
		int maxSpawnDelay = statsWrapper.getOriginalMaxSpawnDelay();
		int newDelay = serverLevel.random.nextInt(maxSpawnDelay - minSpawnDelay + 1) + minSpawnDelay;
	
		CompoundTag spawnerTag = SpawnerUtilityMethods.saveSpawnerToTag(sbe);
		
		// Restore baseline spawn timing fields
		spawnerTag.putInt("MinSpawnDelay", minSpawnDelay);
		spawnerTag.putInt("MaxSpawnDelay", maxSpawnDelay);
		spawnerTag.putInt("Delay", newDelay);
	
		SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
		sbe.setChanged();
		serverLevel.sendBlockUpdated(
			    sbe.getBlockPos(),
			    sbe.getBlockState(),
			    sbe.getBlockState(),
			    3 // flags: 3 = send to clients + render update
			);
	
		if (MyConfig.isDebug()) {
			MyUtilities.debugMsg(1, sbe.getBlockPos(),
					"Spawner restored: min=" + minSpawnDelay + ", max=" + maxSpawnDelay + ", delay=" + newDelay);
		}
	}

}
