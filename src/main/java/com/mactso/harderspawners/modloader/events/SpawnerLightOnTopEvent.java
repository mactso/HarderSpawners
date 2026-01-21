package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerLightLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Handles the case where a block is placed or updated above a spawner, especially 
 * when light-emitting blocks or fluids are above it. 
 * <p>
 * It is intended to prevent spawners from being invalidated or bypassed by placing
 * bright blocks or liquids up to 16 blocks above the spawner.
 */
public class SpawnerLightOnTopEvent {
	/**
	 * Handles updates to blocks above a spawner when neighbors change.
	 * Checks if the block below is a spawner and compares light emission
	 * against the dimension's monster spawn light level.
	 * Destroys blocks exceeding the limit (except Redstone Lamps).
	 * Replaces fluids above with cobblestone or air as appropriate.
	 *
	 * @param event The {@link BlockEvent.NeighborNotifyEvent} triggered by the change.
	 */
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
