package com.mactso.harderspawners.common.logic;

import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contains logic for handling blocks and fluids above spawners.
 */
public class SpawnerLightLogic {

    /**
     * Checks all tracked spawners in the given level for blocks above them
     * that may interfere with mob spawning (light or fluids) and handles them.
     */
    public static void handleAllSpawners(ServerLevel serverLevel) {
        // Iterate over all loaded block entities
        for (BlockEntity be : serverLevel.blockEntities.values()) {
            if (!(be instanceof SpawnerBlockEntity sbe)) continue;

            BlockPos spawnerPos = sbe.getBlockPos();

            // Check up to 16 blocks above the spawner
            for (int yOffset = 1; yOffset <= 16; yOffset++) {
                BlockPos checkPos = spawnerPos.above(yOffset);
                var state = serverLevel.getBlockState(checkPos);

                // Call your existing single-spawner logic
                handleBlockAboveSpawner(serverLevel, checkPos, state);
            }
        }
    }
    
    public static void handleBlockAboveSpawner(ServerLevel serverLevel, BlockPos changedPos, BlockState changedState) {
        BlockPos spawnerPos = changedPos.below();
        if (serverLevel.getBlockState(spawnerPos).getBlock() != Blocks.SPAWNER) return;

        int lightLimit = serverLevel.dimensionType().monsterSpawnBlockLightLimit();
        if (changedState.getLightEmission(serverLevel, spawnerPos) < lightLimit
                && changedState.getBlock() != Blocks.REDSTONE_LAMP) {
            return;
        }

        // Destroy offending block
        serverLevel.destroyBlock(changedPos, true);

        // Cancel event if possible (handled by caller in Neoforge)
        // event.setCanceled(true); -- move this to caller if needed

        if (serverLevel.getFluidState(changedPos).isEmpty()) return;

        // Handle glowing liquids above
        for (int i = 0; i < 16; i++) {
            BlockPos abovePos = changedPos.above(i);
            if (abovePos.getY() > serverLevel.getHeight()) break;

            if (serverLevel.getBlockState(abovePos).getLightEmission(serverLevel, abovePos) < 8) break;

            if (serverLevel.getFluidState(abovePos).isSource()) {
                serverLevel.setBlock(abovePos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            } else {
                serverLevel.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}