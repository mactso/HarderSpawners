package com.mactso.harderspawners.modloader.main;

import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.events.ExtendSpawnerExpirationTimeEvent;
import com.mactso.harderspawners.modloader.events.MobSpawnHandler;
import com.mactso.harderspawners.modloader.events.MyCommandsRegisterEvent;
import com.mactso.harderspawners.modloader.events.MyEntityPlaceEvent;
import com.mactso.harderspawners.modloader.events.ServerEvents;
import com.mactso.harderspawners.modloader.events.SpawnerBreakHandler;
import com.mactso.harderspawners.modloader.events.SpawnerLightOnTopEvent;
import com.mactso.harderspawners.modloader.events.SpawnerSpawnEvent;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerAttachments;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Main entry point for the Harder Spawners mod under Fabric.
 * Handles mod setup, config registration, and event subscriptions.
 */
public class Main implements ModInitializer {

    public static final String MODID = "harderspawners";
    public static final String MOD_VERSION = "v30.6 1.21.3";

    @Override
    public void onInitialize() {

        // --- Config ---
        MyConfig.register();

        // --- Sounds ---
        ModSounds.register();

        // --- Event registration ---
        ExtendSpawnerExpirationTimeEvent.register();
        MobSpawnHandler.register();
        SpawnerBreakHandler.register();
        SpawnerSpawnEvent.register();
        SpawnerLightOnTopEvent.register();
        MyEntityPlaceEvent.register();
        ServerEvents.register();

        // --- Commands ---
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MyCommandsRegisterEvent.register(dispatcher);
        });

        // --- Spawner attachments ---
        SpawnerAttachments.register();

    }
}
