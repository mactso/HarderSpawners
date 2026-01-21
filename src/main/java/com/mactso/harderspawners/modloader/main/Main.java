
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

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Main entry point for the Harder Spawners mod. Handles mod setup, config
 * registration, and event subscriptions.
 */

@Mod("harderspawners")
public class Main {

	public static final String MODID = "harderspawners";
	public static final String MOD_VERSION = "v30.6 1.21.6";

	public Main(IEventBus modEventBus, ModContainer modContainer) {

		NeoForge.EVENT_BUS.register(new MyCommandsRegisterEvent());
		NeoForge.EVENT_BUS.register(new ExtendSpawnerExpirationTimeEvent());
		NeoForge.EVENT_BUS.register(new MobSpawnHandler());
		NeoForge.EVENT_BUS.register(new SpawnerBreakHandler());
		NeoForge.EVENT_BUS.register(new SpawnerSpawnEvent());
		NeoForge.EVENT_BUS.register(new SpawnerLightOnTopEvent());
		NeoForge.EVENT_BUS.register(new MyEntityPlaceEvent());
		NeoForge.EVENT_BUS.register(new ServerEvents());
		
		SpawnerAttachments.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
		ModSounds.SOUND_EVENTS.register(modEventBus);
	}

}