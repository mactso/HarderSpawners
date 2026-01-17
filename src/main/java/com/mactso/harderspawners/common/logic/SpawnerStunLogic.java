package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * Holds spawner stunning logic.
 * Marks spawner as stunned and extends spawn delays plays special effects,
 * Marks spawner as not stunned and restores spawn delays plays special effects,
 */

public class SpawnerStunLogic {
	
    /**
     * Stuns a spawner: updates delays, plays sound, and extends expiration.
     * Expensive: reloads spawner NBT.
     */
	public static void stunSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe, BaseSpawner spawner,
			SpawnerStatsStorage stats, BlockPos pos) {

		// Mark the spawner as stunned
		MyUtilities.debugMsg(1, pos, "Stunning Spawner");
		stats.setStunned(true);
		
		// Play stun sound
		serverLevel.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);

		// Save current spawner state
		CompoundTag tag = new CompoundTag();
		spawner.save(tag);  // <--- expensive

		MyUtilities.debugMsg(2, pos, "Stunned Spawner saved values: (max):" + stats.getOriginalTag().getInt("MinSpawnDelay")
				+ " (min):" + stats.getOriginalTag().getInt("MaxSpawnDelay"));

		// Apply new delays for stunned state
		int stunnedTicks = MyConfig.getSpawnerTicksStunned();
		tag.putInt("MinSpawnDelay", stunnedTicks);
		tag.putInt("MaxSpawnDelay", stunnedTicks + 10);
		tag.putInt("Delay", stunnedTicks + 5);
		
		// extend the spawner expiration time so stunned time doesn't count against it.
		int timestunned = MyConfig.getSpawnerTicksStunned();
		long averagetimeperspawn = stats.getAverageTimePerSpawn();
		if (timestunned > averagetimeperspawn) {
			stats.setSpawnerExpirationTime(
			        stats.getSpawnerExpirationTime() + ((long)timestunned - averagetimeperspawn)
			);
			
		}
		
		sbe.setChanged();
		// Load updated tag into spawner
		spawner.load(serverLevel, pos, tag);  // <----- this is expensive
		sbe.setChanged();

		MyUtilities.debugMsg(1, pos, "Stunned Spawner stunned values: (max):" + tag.getInt("MaxSpawnDelay") + " (min):"
				+ tag.getInt("MinSpawnDelay"));
	}

	  /**
     * Debug print for stunned spawner state vs original tag.
     */
	public static void doStunDebugMsg(BlockEntity sbe, CompoundTag tag,
			SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper) {
		if (MyConfig.getDebugLevel() <= 0) {
			return;
		}
	
		BlockPos pos = sbe.getBlockPos();
		MyUtilities.debugMsg(1, pos, "Restoring Stunned Spawner");
	
		// Read the saved min/max delays from the spawner's original tag
		CompoundTag originalTag = statsWrapper.getOriginalTag();
		int savedMax = originalTag != null ? originalTag.getInt("MaxSpawnDelay") : 0;
		int savedMin = originalTag != null ? originalTag.getInt("MinSpawnDelay") : 0;
	
		MyUtilities.debugMsg(2, pos, "Stunned Spawner tag values: (max):" + tag.getInt("MaxSpawnDelay") + " (min):"
				+ tag.getInt("MinSpawnDelay"));
		MyUtilities.debugMsg(2, pos, "Restoring Spawner original values: (max):" + savedMax + " (min):" + savedMin);
	}

    /**
     * Restores spawner from stunned state: resets min and max spawndelays and resets 'stunned' flag.
     * this is called just before a stunned spawner spawns again.
     */
	public static boolean doSpawnerRecoverFromStun(SpawnerBlockEntity sbe,
			CompoundTag tag,
			SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper ) {
	
		// If spawner is not stunned, nothing to do
		if (!statsWrapper.isStunned()) {
			return false;
		}
	
		// Debug info
		doStunDebugMsg(sbe, tag, statsWrapper);
	
		// Restore original min/max spawn delays from backed-up tag
		CompoundTag originalTag = statsWrapper.getOriginalTag();
		if (originalTag != null) {
			int originalMax = originalTag.getInt("MaxSpawnDelay");
			int originalMin = originalTag.getInt("MinSpawnDelay");
			tag.putInt("MaxSpawnDelay", originalMax);
			tag.putInt("MinSpawnDelay", originalMin);
		}
		
		// Load the restored tag into the spawner  <--- this is expensive but 1 to 27 minutes apart.
		sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), tag);

		// Play spooky “spawner spawner recovers” sound.  this is the minecraft villager infected sound.
		sbe.getLevel().playSound(null, sbe.getBlockPos(), ModSounds.SPAWNER_RECOVERS.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
	
		// Set spawner stats to not stunned
		statsWrapper.setStunned(false);
	
		return true;
	}
}
