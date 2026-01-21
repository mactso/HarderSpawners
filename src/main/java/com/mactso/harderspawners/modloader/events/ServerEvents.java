package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.managers.MobSpawnerManager;
import com.mactso.harderspawners.common.managers.SpawnerPositionManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerEvents {

    public static void register() {
        // Called when the server is about to start
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            MobSpawnerManager.init();
        });

        // Called when the server is stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SpawnerPositionManager.clearSpawnerLocations();
        });
    }
}