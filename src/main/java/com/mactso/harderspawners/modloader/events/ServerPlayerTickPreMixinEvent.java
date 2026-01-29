package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockAndFluidPlacement;
import com.mactso.harderspawners.common.logic.ProcessSpawners;

import net.minecraft.server.level.ServerPlayer;

public class ServerPlayerTickPreMixinEvent {

	public static void register () {
		// this is not an event but is called by a mixin;
	}
	
	public static void handleEvent (ServerPlayer sp){
        BlockAndFluidPlacement.removePendingBrightFluid(sp);
        BlockAndFluidPlacement.clearPendingBrightBlocks(sp);
        ProcessSpawners.findAndProcessNearbySpawners(sp);
    }
}