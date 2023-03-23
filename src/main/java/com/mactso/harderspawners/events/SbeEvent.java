package com.mactso.harderspawners.events;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SbeEvent
{
    @SubscribeEvent
    public void onAttach(AttachCapabilitiesEvent<BlockEntity> event)
    {
        BlockEntity be = event.getObject();
        if (be instanceof SpawnerBlockEntity sbe)
        {
            ServerTickHandler.addSbeWorklistEntry(sbe);
        }
    }
}
