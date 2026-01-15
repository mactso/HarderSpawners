package com.mactso.harderspawners.modloader.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
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

		if (!(event.getNotifiedSides().contains(Direction.DOWN)))
			return;

		if (!(event.getLevel() instanceof ServerLevel serverLevel))
			return;

		if (serverLevel.getBlockState(event.getPos().below()).getBlock() != Blocks.SPAWNER)
			return;

		// check that the block isn't emitting light at the spawner location
		// chatgpt, please get the dimension spawning light level from the serverLevel.
		int dimensionSpawningLightLimit = serverLevel.dimensionType().monsterSpawnBlockLightLimit();
		BlockState bS = event.getState();
		if ((bS.getLightEmission(serverLevel, event.getPos().below()) < dimensionSpawningLightLimit)
				&& (bS.getBlock() != Blocks.REDSTONE_LAMP)) {
			return;
		}

		serverLevel.destroyBlock(event.getPos(), true);
		event.setCanceled(true);

		if (event.getLevel().getFluidState(event.getPos()).isEmpty()) {
			return;
		}

		// search for glowing liquid coming from above.

		for (int i = 0; i < 16; i++) {
		    BlockPos abovePos = event.getPos().above(i);

		    if (abovePos.getY() > serverLevel.getMaxBuildHeight()) break;

		    if (serverLevel.getBlockState(abovePos).getLightEmission(serverLevel, abovePos) < 8) break;

		    if (serverLevel.getFluidState(abovePos).isSource()) {
		        serverLevel.setBlock(abovePos, Blocks.COBBLESTONE.defaultBlockState(), 3);
		    } else {
		        serverLevel.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
		    }
		}

	}
}
