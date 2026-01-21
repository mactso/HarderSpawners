package com.mactso.harderspawners.common.logic;

import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contains logic for handling blocks and fluids above spawners.
 */
public class SpawnerLightLogic {

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