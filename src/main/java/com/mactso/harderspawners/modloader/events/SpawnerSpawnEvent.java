package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.ProcessSpawners;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpawnerSpawnEvent {

	
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp))
            return;

        // Delegate all the logic to the common class
        ProcessSpawners.clearPendingLava(sp);
        ProcessSpawners.findAndProcessNearbySpawners(sp);
    }
}