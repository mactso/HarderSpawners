package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockAndFluidPlacement;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

public class BlockPlacedEvent {

    // this queues "bright" blocks for destruction next tick.
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

        	InteractionResult ir =  BlockAndFluidPlacement.breakLightEmittingBlocksEvent(player,world,hand,hitResult);
            return ir; // <-- merely a "guideline".
        });
    }

}

