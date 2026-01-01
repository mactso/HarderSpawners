package com.mactso.harderspawners.events;

import com.mactso.harderspawners.Main;
import com.mactso.harderspawners.capabilities.SpawnerStatsStorageProvider;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
	    modid = Main.MODID,
	    bus = Mod.EventBusSubscriber.Bus.FORGE
	)

public class SbeAttachEvent
{
	private static final Identifier KEY = Identifier.fromNamespaceAndPath(Main.MODID, "spawnerstatssapability");
	
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent.BlockEntities event)
    {
        BlockEntity be = event.getObject();
        if (be instanceof SpawnerBlockEntity sbe)
        {
        	ServerTickHandler.addSbeWorklistEntry(sbe);
        	event.addCapability(KEY, new SpawnerStatsStorageProvider());
        }
    }
}
