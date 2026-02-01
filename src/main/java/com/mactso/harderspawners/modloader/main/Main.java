package com.mactso.harderspawners.modloader.main;

import com.mactso.harderspawners.common.commands.MyCommands;
import com.mactso.harderspawners.common.sounds.ModSounds;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.events.AddSpawnerLifespanEvent;
import com.mactso.harderspawners.modloader.events.BlockGlowingFluidEvent;
import com.mactso.harderspawners.modloader.events.BlockPlacedEvent;
import com.mactso.harderspawners.modloader.events.ServerEvents;
import com.mactso.harderspawners.modloader.events.ServerPlayerTickPreMixinEvent;
import com.mactso.harderspawners.modloader.events.SpawnerBreakEvent;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Main entry point for the Harder Spawners mod under Fabric. Handles mod setup,
 * config registration, and event subscriptions.
 */
public class Main implements ModInitializer {

	public static final String MODID = "harderspawners";
	public static final String MOD_VERSION = "v31.3 1.21.10";

	@Override
	public void onInitialize() {

		registerEvents();
		// --- Config ---
		MyConfig.registerConfigs();
		
	}

	/** Register Fabric event callbacks */
	private void registerEvents() {
		
		ModSounds.register();

		// --- Event registration ---
		BlockPlacedEvent.register();
		AddSpawnerLifespanEvent.register();
		// MobSpawnHandler.register(); replaced by MobSpawnHandlerMixin
		SpawnerBreakEvent.register();
		ServerPlayerTickPreMixinEvent.register();
		// SpawnerLightOnTopEvent.register();  replaced by call to SpawnerLightLogic.removeLightSourcesAboveASpawner in ProcessSpawners
		BlockGlowingFluidEvent.register();
		ServerEvents.register();

		// --- Commands ---
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MyCommands.register(dispatcher);
        });


	}

	// --- Sounds ---

}
