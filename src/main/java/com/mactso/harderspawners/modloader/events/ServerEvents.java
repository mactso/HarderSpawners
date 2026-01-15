package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerRegistry;
import com.mactso.harderspawners.common.managers.MobSpawnerManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class ServerEvents {

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event)
	{
    	SpawnerRegistry.clearSpawnerLocations();
	}
	
    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Initialize durability config here
        MobSpawnerManager.init();
    }
}


