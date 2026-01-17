package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockFluidPlacementLogic;
import com.mactso.harderspawners.common.logic.ProcessSpawners;
import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.common.utility.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class MyEntityPlaceEvent {

	@SubscribeEvent
	public void bucket(PlayerInteractEvent.RightClickBlock event) {

		if (!(event.getEntity() instanceof ServerPlayer sp))
			return;
		ServerLevel sLevel = (ServerLevel) sp.level();
		ItemStack stack = event.getItemStack();

		if (!(stack.getItem() instanceof BucketItem bucket))
			return;

		BlockPos clickedPos = event.getPos();
		Direction clickedFace = event.getFace();

		// Delegate to the common logic class
		boolean shouldCancel = BlockFluidPlacementLogic.handleBucketPlacement(sp, stack, clickedPos, clickedFace);

		if (shouldCancel) {
			BlockPos targetPos = clickedPos.relative(clickedFace);
			ProcessSpawners.queuePendingLava(sLevel, targetPos);
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	@SubscribeEvent
	public void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {

		if (!(event.getEntity() instanceof ServerPlayer sp))
			return;

		boolean destroyedBlock = BlockFluidPlacementLogic.handleBlockPlacement(sp, event.getPlacedBlock(),
				event.getPos());
		if (destroyedBlock)
			MyUtilities.debugMsg(1, event.getPos(), "Destroyed placed block " + event.getPlacedBlock().getBlock() );

	}



}
