package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerLifespan;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class AddSpawnerLifespanEvent {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

        	// Pass gives the event back for further processing.
            // Success consumes and stops processing of the event..

            if (!(player instanceof ServerPlayer serverPlayer))
            	return InteractionResult.PASS;

            if (!(world instanceof ServerLevel serverLevel))
            	return InteractionResult.PASS;
            
            ItemStack stack = player.getItemInHand(hand);            
            if (!SpawnerLifespan.isExtraLifespanItem(stack, serverLevel))
            	return InteractionResult.PASS;

            if (hitResult == null) 
            	return InteractionResult.PASS;
            
            BlockPos pos = hitResult.getBlockPos();
            
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);

            // Only trigger for SpawnerBlockEntity
            if (!(blockEntity instanceof SpawnerBlockEntity sbe)) {
                return InteractionResult.PASS;
            }


            handleSpawnerRightClick(serverLevel, serverPlayer, pos, sbe, stack);
            return InteractionResult.SUCCESS;
        });
    }

    private static void handleSpawnerRightClick(ServerLevel serverLevel, ServerPlayer serverPlayer, BlockPos pos, SpawnerBlockEntity sbe, ItemStack stack) {

    	SpawnerLifespan.addSpawnerLifespan(serverLevel, serverPlayer, pos, sbe, stack);
    	
    }
}