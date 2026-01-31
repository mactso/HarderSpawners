package com.mactso.harderspawners.modloader.events;

import java.util.List;

import com.mactso.harderspawners.common.logic.ExplosionDetonateEventLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ExplosionDetonateEvent {

    private ExplosionDetonateEvent() {
        /* bridge class */
    }

    public static void protectBlocks(
            ServerLevel level,
            List<BlockPos> blocks
    ) {
        protectSpawnersFromExplosion(level, blocks);
    }

    public static void protectSpawnersFromExplosion(
            ServerLevel level,
            List<BlockPos> blocks
    ) {
        ExplosionDetonateEventLogic.protectSpawnersFromExplosion(level, blocks);
    }
}
