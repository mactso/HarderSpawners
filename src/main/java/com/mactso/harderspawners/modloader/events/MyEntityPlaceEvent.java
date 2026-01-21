package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockFluidPlacementLogic;
import com.mactso.harderspawners.common.utility.MyUtilities;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.BlockPlaceCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

public class MyEntityPlaceEvent {

    public static void register() {
        // Right-click (bucket) event
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer sp))
                return InteractionResult.PASS;

            ServerLevel sLevel = (ServerLevel) sp.level();
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof BucketItem bucket))
                return InteractionResult.PASS;

            BlockPos clickedPos = hitResult.getBlockPos();
            Direction clickedFace = hitResult.getDirection();

            boolean shouldCancel = BlockFluidPlacementLogic.handleBucketPlacement(sp, stack, clickedPos, clickedFace);
            if (shouldCancel) {
                BlockPos targetPos = clickedPos.relative(clickedFace);
                BlockFluidPlacementLogic.queuePendingLava(sLevel, targetPos);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Block placement event
        BlockPlaceCallback.EVENT.register((player, world, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp))
                return false;

            boolean destroyedBlock = BlockFluidPlacementLogic.handleBlockPlacement(sp, state, pos);
            if (destroyedBlock) {
                MyUtilities.debugMsg(1, pos, "Destroyed placed block " + state.getBlock());
            }
            return false; // false = allow normal placement
        });
    }
}
