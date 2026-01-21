package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
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

/**
 * Holds spawner stunning logic. Marks spawner as stunned and extends spawn
 * delays plays special effects, Marks spawner as not stunned and restores spawn
 * delays plays special effects,
 */

public class SpawnerStunLogic {

	/**
	 * Stuns a spawner: updates delays, plays sound, and extends expiration.
	 * Expensive: reloads spawner NBT.
	 */
	public static void stunSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe, SpawnerStatsWrapper wrapper) {

		BlockPos pos = sbe.getBlockPos();
		MyUtilities.debugMsg(1, pos, "Stunning Spawner");
		wrapper.setStunned(true);

		// Play stun sound
		serverLevel.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);

		// load current spawner state into tag
		CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);

		MyUtilities.debugMsg(2, pos, "Stunned Spawner saved values: (min):" + wrapper.getOriginalMinSpawnDelay()
				+ " (max):" + wrapper.getOriginalMaxSpawnDelay());

		// Apply new "stunned" min and max delays
		int stunnedTicks = MyConfig.getSpawnerTicksStunned();
		spawnerTag.putInt("MinSpawnDelay", stunnedTicks);
		spawnerTag.putInt("MaxSpawnDelay", stunnedTicks + 10);
		spawnerTag.putInt("Delay", stunnedTicks + 5);
		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, pos, "Stunned Spawner stunned values: (min):" + spawnerTag.getInt("MinSpawnDelay")
					+ " (max):" + spawnerTag.getInt("MaxSpawnDelay"));

		// Apply modified spawner tag
		SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
		sbe.setChanged();

	}

	public static void lingerStunSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe, SpawnerStatsWrapper wrapper, CompoundTag spawnerTag) {

		final int STUN_TICKS = 25 * 60 * 20; // 25 minutes in ticks

		BlockPos pos = sbe.getBlockPos();
		MyUtilities.debugMsg(1, pos, "Stunning Spawner (linger)");

		// Mark as stunned
		wrapper.setStunned(true);
		serverLevel.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.AMBIENT, 1.0f, 1.0f); 		// Play linger stun sound
		// Apply "stunned" delays
		spawnerTag.putInt("MinSpawnDelay", STUN_TICKS);
		spawnerTag.putInt("MaxSpawnDelay", STUN_TICKS + 10);
		// Load modified tag back into spawner
		SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);
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
	 * Restores spawner from stunned state: resets min and max spawndelays and
	 * resets 'stunned' flag. this is called just before a stunned spawner spawns
	 * again.
	 */
	public static boolean doSpawnerRecoverFromStun(SpawnerBlockEntity sbe, CompoundTag spawnerTag,
			SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper) {

	    MyUtilities.debugMsg(1, "running doSpawnerRecoverFromStun\n");
		if (!statsWrapper.isStunned())
			return false;

		doStunDebugMsg(sbe, spawnerTag, statsWrapper);

		// Restore original min/max spawn delays from wrapper cache
		int originalMin = statsWrapper.getOriginalMinSpawnDelay();
		int originalMax = statsWrapper.getOriginalMaxSpawnDelay();

		spawnerTag.putInt("MinSpawnDelay", originalMin);
		spawnerTag.putInt("MaxSpawnDelay", originalMax);

		// Load the restored tag into the spawner
		SharedUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);

		sbe.getLevel().playSound(null, sbe.getBlockPos(), ModSounds.SPAWNER_RECOVERS.value(), SoundSource.BLOCKS, 1.0f,
				1.0f);

		statsWrapper.setStunned(false);

		return true;
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

		if (wrapper.isStunned()) {
			BlockPos pos = sbe.getBlockPos();
			serverLevel.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.AMBIENT, 1.0f, 1.0f);
			return true;
		}

		stunSpawner(serverLevel, sbe, wrapper);

		return true;
	}

}
