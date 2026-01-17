package com.mactso.harderspawners.common.utility;

import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerAttachments;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsStorage;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public class SharedUtilityMethods {

	private static final org.slf4j.Logger LOGGERUTIL =  LogUtils.getLogger();
	
	/**
	 * Utility methods shared across the Harder Spawners mod.
	 * 
	 * <p>Includes helper functions for spawner status, block brightness adjustments,
	 * and destruction of lights near blocks.</p>
	 * 
	 * <p><strong>Important:</strong> Some methods are computationally expensive but
	 * are safe because they are throttled:
	 * <ul>
	 *   <li>{@link #doDestroyLightingNearSpawner(BlockPos, ServerLevel)} is only called
	 *       when a spawner is about to spawn (spawnDelay == 1) or when a player attempts
	 *       to break a spawner.</li>
	 *   <li>This ensures that even with 3D scanning of nearby blocks, the performance
	 *       impact is limited.</li>
	 * </ul>
	 * </p>
	 */

	public static CompoundTag saveSpawnerToTag(SpawnerBlockEntity sbe) {
	    ScopedCollector problemReporter = new ScopedCollector(LOGGERUTIL);
	    TagValueOutput output = TagValueOutput.createWithoutContext(problemReporter);
	    sbe.getSpawner().save(output);
	    return output.buildResult();
	}
	
	public static void loadSpawnerFromTag(SpawnerBlockEntity sbe, CompoundTag tag) {
	    ScopedCollector collector = new ScopedCollector(LOGGERUTIL);
	    ValueInput input = TagValueInput.create(collector, sbe.getLevel().registryAccess(), tag);
	    sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), input);
	}
	
	
	public static boolean isSpawnerStunned(SpawnerBlockEntity sbe) {
		SpawnerStatsStorage stats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());
		return stats != null && stats.isStunned();
	}

    /**
     * Adjusts the effective light level of a block, taking certain redstone components
     * into account (which can provide maximum brightness regardless of the normal light emission).
     *
     * @param world the level containing the block
     * @param placedBlockState the state of the block
     * @param placedBlock the block type
     * @param placedPos the position of the block
     * @return the effective light emission of the block
     */
	public static int checkAdjustedBlockBrightness(Level world, BlockState placedBlockState, Block placedBlock,
			BlockPos placedPos) {

		if (placedBlock instanceof RedstoneLampBlock)
			return 15;
		if (placedBlock instanceof PoweredBlock)
			return 15;
		if (placedBlock instanceof RedStoneWireBlock)
			return 15;
		if (placedBlock instanceof LeverBlock)
			return 15;
		if (placedBlock instanceof RepeaterBlock)
			return 15;
		if (placedBlock instanceof ComparatorBlock)
			return 15;

		return placedBlockState.getLightEmission(world, placedPos);

	}


    /**
     * Destroys light sources near the given block position if they exceed the
     * hostile spawner light level limit.
     * Note: This *only* runs when someone tries to break a spawner or when the
	 * spawner tries to spawn.
     *
     * <p><strong>Trigger conditions:</strong>
     * <ul>
     *   <li>Called when a player attempts to break a spawner block.</li>
     *   <li>Called when a spawner is about to spawn (spawnDelay == 1).</li>
     * </ul>
     * 
     * <p>The method scans a cubic area around the target block and may:
     * <ul>
     *   <li>Destroy blocks emitting light above the configured level.</li>
     *   <li>Extinguish fluids such as lava while playing a sound effect.</li>
     * </ul>
     * 
     * <p>Although this method performs a potentially expensive 3D scan, the
     * performance impact is minimal due to the throttled trigger conditions.
     *
     * @param pos the block position to scan around
     * @param sLevel the server level containing the block
     * @return true if any lights or lava were destroyed
     */
	
	public static boolean doDestroyLightingNearSpawner( SpawnerBlockEntity sbe) {

		
		Level level = sbe.getLevel();
		if ((level == null) || (!(level instanceof ServerLevel serverLevel)))
				return false;
		
		if (serverLevel.dimensionType().monsterSpawnBlockLightLimit() == 15)
			return false;

		BlockPos pos = sbe.getBlockPos();
		
		if (serverLevel.getBrightness(LightLayer.SKY, pos) == 15)
			return false;

		if (serverLevel.getMaxLocalRawBrightness(pos) < 1)
			return false;


		RandomSource rand = serverLevel.getRandom();
		int fYmin = (int) pos.getY() - 4;
		if (fYmin < serverLevel.getHeight())
			fYmin = serverLevel.getHeight();
		int fYmax = (int) pos.getY() + 8;
		if (fYmax > serverLevel.getMinY())
			fYmax = serverLevel.getMinY();
		
		int scanSize = MyConfig.getDestroyLightRange();
		int lavaScanBoost = 0;
		boolean destroyedLight = false;
		boolean destroyedLava = false;
		int customLightLevel = MyConfig.getHostileSpawnerLightLevel();

		MutableBlockPos mutPos = pos.mutable();
		for (int dy = fYmin; dy <= fYmax + scanSize + lavaScanBoost; dy++) {
			for (int dx = pos.getX() - scanSize; dx <= pos.getX() + scanSize; dx++) {
				for (int dz = pos.getZ() - scanSize; dz <= pos.getZ() + scanSize; dz++) {

					if (rand.nextInt(100)>MyConfig.getDestroyLightPercentage())
						continue;
					
					mutPos.setX(dx);
					mutPos.setY(dy);
					mutPos.setZ(dz);

					BlockState bs = serverLevel.getBlockState(mutPos);
					Block b = bs.getBlock();
					// Debugging Code
					// SimpleParticleType particles = ParticleTypes.END_ROD;
					// slevel.sendParticles(particles, dx, dy, dz, 3, 0, 0, 0, -0.04D);
					int blockLightLevel = checkAdjustedBlockBrightness(serverLevel, bs, b, mutPos);

					if (blockLightLevel > customLightLevel) {
						if (b != Blocks.END_PORTAL) {
							serverLevel.destroyBlock(mutPos, true);
							destroyedLight = true;
						}
					}
					Fluid f = serverLevel.getFluidState(mutPos).getType();
					if (f.getFluidType().getLightLevel() > 0) {
						serverLevel.playSound(null, mutPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f, 0.25f);
						serverLevel.setBlock(mutPos, Blocks.AIR.defaultBlockState(), 3);
						lavaScanBoost = 4; // found lava- scan a larger vertical area.
						destroyedLava = true;
						destroyedLight = true;
					}
				}
			}
		}
		if (destroyedLava) 
			MyUtilities.debugMsg(2, pos, "Destroyed lava near spawner.");
		return destroyedLight;
	}

}
