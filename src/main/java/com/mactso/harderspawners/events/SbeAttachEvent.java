package com.mactso.harderspawners.events;

import com.mactso.harderspawners.Main;
import com.mactso.harderspawners.capabilities.SpawnerStatsStorageProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber() 
public class SbeAttachEvent
{
	private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(Main.MODID, "spawnerstatssapability");
	
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
