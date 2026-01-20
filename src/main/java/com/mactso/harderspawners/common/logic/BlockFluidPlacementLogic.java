package com.mactso.harderspawners.common.logic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class BlockFluidPlacementLogic {

	/**
	 * Handles placement of a bucket fluid near a spawner. Returns true if the event
	 * should be canceled.
	 */
	public static boolean handleBucketPlacement(ServerPlayer sp, ItemStack stack, BlockPos clickedPos, Direction face) {

		if (!(sp.level() instanceof ServerLevel sLevel))
			return false;

		if (clickedPos == null || face == null)
			return false;

		if (stack == null || !(stack.getItem() instanceof BucketItem bucket))
			return false;

		Fluid fluid = bucket.content;
		if (fluid == null || fluid.getFluidType() == null || fluid.getFluidType().getLightLevel() == 0) {
			return false; // skip non-bright fluids
		}

		ProcessSpawners.findAndProcessNearbySpawners(sp);

		BlockPos placedPos = clickedPos.relative(face);

		if (!SpawnerRegistry.isSpawnerNearby(sLevel, placedPos, MyConfig.getDestroyLightRange()))
			return false;

		// Play sound and particles
		sLevel.playSound(null, placedPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f, 0.25f);
		doLavaPlacementFailParticles(sLevel, placedPos, face);

		return true; // cancel the event
	}

	private static void doLavaPlacementFailParticles(ServerLevel sLevel, BlockPos pos, Direction direction) {
		RandomSource rand = sLevel.getRandom();
		double offset = direction == Direction.DOWN ? 0.70d : 0.0d;

		int numParticles = 27;
		for (int j = 0; j < numParticles; ++j) {
			double vx = (0.3d * rand.nextDouble()) - 0.15d;
			double vz = (0.3d * rand.nextDouble()) - 0.15d;
			double vy = 0.1 + rand.nextDouble() / 32;
			double x = pos.getX() + rand.nextDouble();
			double y = pos.getY() + 0.15d + offset;
			double z = pos.getZ() + rand.nextDouble();

			SimpleParticleType particles = ParticleTypes.SMOKE;
			sLevel.sendParticles(particles, x, y, z, 3, vx, vy, vz, -0.04D);
		}
	}

	/**
	 * Handles block placement logic near a spawner. Returns true if the block
	 * should be destroyed.
	 */
	public static boolean handleBlockPlacement(ServerPlayer sp, BlockState placedBlockState, BlockPos placedPos) {
		if (placedBlockState == null || placedPos == null)
			return false;

		if (!(sp.level() instanceof ServerLevel sLevel))
			return false;

		// Random chance to ignore
		if (sLevel.getRandom().nextInt(100) > MyConfig.getDestroyLightPercentage())
			return false;

		Block placedBlock = placedBlockState.getBlock();

		// Skip if the block produces no light
		if (SharedUtilityMethods.checkAdjustedBlockBrightness(sLevel, placedBlockState, placedBlock, placedPos) == 0)
			return false;

		// Skip if maximum local brightness is 15
		if (sLevel.getMaxLocalRawBrightness(placedPos) == 15)
			return false;

		ProcessSpawners.findAndProcessNearbySpawners(sp);

		// Skip if no nearby spawner
		if (!SpawnerRegistry.isSpawnerNearby(sLevel, placedPos, MyConfig.getDestroyLightRange()))
			return false;

		sLevel.destroyBlock(placedPos, true);

		return true; // bright block should be destroyed
	}

	static final Map<ServerLevel, Set<BlockPos>> pendingLavaBlocks = new ConcurrentHashMap<>();

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
}
