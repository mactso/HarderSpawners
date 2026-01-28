//package com.mactso.harderspawners.modloader.events;
//
//import com.mactso.harderspawners.common.logic.SpawnerLightLogic;
//
//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
//import net.minecraft.server.level.ServerLevel;
//
///**
// * Handles the case where a block above a spawner changes, especially
// * for light-emitting blocks or fluids.
// * Converted from NeoForge neighbor notify event.
// */
//public final class SpawnerLightOnTopEvent {
//
//    private SpawnerLightOnTopEvent() {}
//
//    /**
//     * Registers the tick-based callback.
//     * Call this in Main.onInitialize().
//     */
//    public static void register() {
//        ServerTickEvents.END_WORLD_TICK.register(world -> {
//            if (!(world instanceof ServerLevel serverLevel)) return;
//
//            // Check spawners in this level
//            SpawnerLightLogic.handleAllSpawners(serverLevel);
//        });
//    }
//}
