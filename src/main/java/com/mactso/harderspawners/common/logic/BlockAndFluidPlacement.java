package com.mactso.harderspawners.common.logic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.events.BlockGlowingFluidEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Handles player bucket placement near a spawner. Cancels lava bucket placement
 * if too close to a spawner. Plays sound and smoke particles to indicate
 * blocked action. Returns true if the placement should be canceled. Ensures
 * server-side execution and correct spawner proximity checks.
 */

public class BlockAndFluidPlacement {
	
	static final Map<ServerLevel, Set<BlockPos>> pendingBrightBlocks = new ConcurrentHashMap<>();
	static final Map<ServerLevel, Set<BlockPos>> pendingBrightFluid = new ConcurrentHashMap<>();

	
    /**
     * Breaks light emitting blocks placed near the spawner by players.
     * and if a spawner is nearby. Queues blocks for later destruction if necessary.
     * Returns an InteractionResult that the UseBlockCallback can return directly.
     */
    public static InteractionResult breakLightEmittingBlocksEvent(Player player, Level world, 
                                                     net.minecraft.world.InteractionHand hand, 
                                                     BlockHitResult hitResult) {
        // Only process on server
        if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        // Only process for players
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return InteractionResult.PASS;

        // Check the item in hand
        ItemStack handStack = sp.getItemInHand(hand);
        if (!(handStack.getItem() instanceof BlockItem blockItem)) return InteractionResult.PASS;

        // Determine block placement position
        BlockPos clickedPos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();
        BlockPos placedPos = clickedPos.relative(face);
        // Skip if no spawner is nearby
		ProcessSpawners.findAndProcessNearbySpawners(sp); // TODO this may be redundant to serverplayertick
        if (!SpawnerPositionManager.isSpawnerNearby(serverLevel, placedPos, MyConfig.getDestroyLightRange() ))
            return InteractionResult.PASS;

        // Get the block state that would be placed
        BlockState placedState = blockItem.getBlock().defaultBlockState();

        // If the block emits light, queue it for destruction
        // In Fabric, we cannot break it immediately. Fabric is inconsistent about 
        // respecting PASS and FAIL results.
        if (SpawnerLightLogic.getAdjustedBlockStateLightEmission(placedState) > 0) {
            queuePendingBrightBlocks(serverLevel, placedPos);
            return InteractionResult.PASS; // Cancel placement for bright blocks
        }

        // Allow vanilla placement
        return InteractionResult.PASS;
    }



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

		if (!(BlockGlowingFluidEvent.isBrightFluid(bucket))) {
			return false;
		}

		
		ProcessSpawners.findAndProcessNearbySpawners(sp); // TODO this may be redundant to serverplayertick

		if (!SpawnerPositionManager.isSpawnerNearby(sLevel, clickedPos, MyConfig.getDestroyLightRange()))
			return false;
		

		// Play sound and smoke particles at the given position to indicate failed lava
		// placement.
		BlockPos placedPos = clickedPos.relative(face);
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

	
	public static void queuePendingBrightBlocks(ServerLevel level, BlockPos pos) {
		pendingBrightBlocks.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(pos);

	}
	
	public static void removePendingBrightBlocks(ServerPlayer sp) {

		ServerLevel serverLevel = (ServerLevel) sp.level();
		// Get the pending lava set for this level
		Set<BlockPos> pending = pendingBrightBlocks.get(serverLevel);
		if (pending == null || pending.isEmpty()) {
			return; // Nothing to do
		}

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, "Clearing Lava");

		for (BlockPos pos : pending) {
			// Remove the bright fluid block (flash was displayed)
            // Destroy the block immediately, dropping items
            serverLevel.destroyBlock(pos, true); // true = drop items

		}

		// Clear the set so we don't process the same blocks again
		pending.clear();
	}
	
	/**
	 * Handles block placement logic near a spawner. Randomly destroys blocks that
	 * emit light near spawners to prevent spawner abuse. Returns true if the block
	 * was destroyed, false otherwise.
	 */
	public static boolean handleBlockPlacement(ServerPlayer sp, BlockState placedBlockState, BlockPos placedPos) {
		if (placedBlockState == null || placedPos == null)
			return false;

		if (!(sp.level() instanceof ServerLevel sLevel))
			return false;

		// Random chance to ignore
		if (sLevel.getRandom().nextInt(100) > MyConfig.getDestroyLightPercentage())
			return false;

		// Skip if the block produces no light
		if (SpawnerLightLogic.getAdjustedBlockStateLightEmission(placedBlockState) == 0)
			return false;

		// Skip if maximum local brightness is 15
		if (sLevel.getMaxLocalRawBrightness(placedPos) == 15)
			return false;

		ProcessSpawners.findAndProcessNearbySpawners(sp);

		// Skip if no nearby spawner
		if (!SpawnerPositionManager.isSpawnerNearby(sLevel, placedPos, MyConfig.getDestroyLightRange()))
			return false;

		sLevel.destroyBlock(placedPos, true);

		return true; // bright block should be destroyed
	}


	/**
	 * Adds a lava block to the pending queue to be processed on the next player
	 * tick.
	 */
	public static void queuePendingBrightFluid(ServerLevel level, BlockPos pos) {
		pendingBrightFluid.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(pos);

	}

	/**
	 * Clears all queued pending lava blocks for the given player level. Should be
	 * called once per player tick.
	 *
	 * @param sp The server player whose level will be processed.
	 */
	public static void removePendingBrightFluid(ServerPlayer sp) {

		ServerLevel serverLevel = (ServerLevel) sp.level();
		// Get the pending lava set for this level
		Set<BlockPos> pending = pendingBrightFluid.get(serverLevel);
		if (pending == null || pending.isEmpty()) {
			return; // Nothing to do
		}

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, "Clearing Lava");

		for (BlockPos pos : pending) {
			// Remove the bright fluid block (flash was displayed)
			serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

		}
		// Clear the set so we don't process the same blocks again
		pending.clear();
	}
}
