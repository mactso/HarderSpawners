package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockFluidPlacementLogic;
import com.mactso.harderspawners.common.logic.ProcessSpawners;

import net.minecraft.server.level.ServerPlayer;

public class ServerPlayerTickPreMixinEvent {

	public static void register () {
		// this is not an event but is called by a mixin;
	}
	
	public static void handleEvent (ServerPlayer sp){
        BlockFluidPlacementLogic.clearPendingLava(sp);
        ProcessSpawners.findAndProcessNearbySpawners(sp);
    }
}