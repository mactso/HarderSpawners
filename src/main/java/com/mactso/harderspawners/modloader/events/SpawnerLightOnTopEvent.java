package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerLightLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class SpawnerLightOnTopEvent {

    @SubscribeEvent
    public void onNeighborNotifyEvent(BlockEvent.NeighborNotifyEvent event) {

        // Extract event fields
        ServerLevel serverLevel = event.getLevel() instanceof ServerLevel sl ? sl : null;
        BlockPos changedPos = event.getPos();
        BlockState changedState = event.getState();
        boolean notifiedDown = event.getNotifiedSides().contains(Direction.DOWN);

        if (serverLevel == null || !notifiedDown) return;

        SpawnerLightLogic.handleBlockAboveSpawner(serverLevel, changedPos, changedState);
    }
}
