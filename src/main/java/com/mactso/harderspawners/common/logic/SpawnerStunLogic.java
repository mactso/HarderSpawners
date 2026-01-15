package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerStunLogic {
	
	public static void stunSpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe, BaseSpawner spawner,
			SpawnerStatsStorage stats, BlockPos pos) {

		// Mark the spawner as stunned
		MyUtilities.debugMsg(1, pos, "Stunning Spawner");
		stats.setStunned(true);
		
		// Play stun sound
		serverLevel.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);

		// Save current spawner state
		CompoundTag tag = new CompoundTag();
		spawner.save(tag);

		// Debug original stored delays
		MyUtilities.debugMsg(2, pos, "Stunned Spawner saved values: (max):" + stats.getOriginalTag().getInt("MinSpawnDelay")
				+ " (min):" + stats.getOriginalTag().getInt("MaxSpawnDelay"));

		// Apply new delays for stunned state
		int stunnedTicks = MyConfig.getSpawnerTicksStunned();
		tag.putInt("MinSpawnDelay", stunnedTicks);
		tag.putInt("MaxSpawnDelay", stunnedTicks + 10);
		tag.putInt("Delay", stunnedTicks + 5);

		// Load updated tag into spawner
		spawner.load(serverLevel, pos, tag);  // <----- this is expensive
		sbe.setChanged();

		MyUtilities.debugMsg(1, pos, "Stunned Spawner stunned values: (max):" + tag.getInt("MaxSpawnDelay") + " (min):"
				+ tag.getInt("MinSpawnDelay"));
	}

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

	// this is called just before a stunned spawner spawns again.
	// it restores the normal min and max spawn delays to the spawner.
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
	
		// Load the restored tag into the spawner  <--- this is expensive.
		sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), tag);
	
		// Set spawner stats to not stunned
		statsWrapper.setStunned(false);
	
		return true;
	}
}
