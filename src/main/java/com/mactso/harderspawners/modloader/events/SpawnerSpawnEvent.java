package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.BlockFluidPlacementLogic;
import com.mactso.harderspawners.common.logic.ProcessSpawners;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles per-player tick logic for spawner processing.
 * Converted from NeoForge PlayerTickEvent.
 */
public final class SpawnerSpawnEvent {

    private SpawnerSpawnEvent() {}

    /** Registers the player tick callback. Call this in Main.onInitialize(). */
    public static void register() {
        ServerTickEvents.START_PLAYER_TICK.register(player -> {
            if (!(player instanceof ServerPlayer sp)) return;

            // Delegate all the logic to the common class
            BlockFluidPlacementLogic.clearPendingLava(sp);
            ProcessSpawners.findAndProcessNearbySpawners(sp);
        });
    }
}