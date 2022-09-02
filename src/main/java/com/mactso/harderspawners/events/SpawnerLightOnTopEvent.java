package com.mactso.harderspawners.events;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerLightOnTopEvent {
	@SubscribeEvent
	public void onNeighborNotifyEvent (BlockEvent.NeighborNotifyEvent event) {

		if (!(event.getNotifiedSides().contains(Direction.DOWN))) {
			return;
		}
		
    	if (event.getLevel().getBlockState(event.getPos().below()).getBlock() != Blocks.SPAWNER) {
    		return;
    	};
    	
		BlockState bS = event.getState();
    	if ((bS.getLightEmission(event.getLevel(), event.getPos().below()) < 8)&&(bS.getBlock() != Blocks.REDSTONE_LAMP)) {
    		return;
    	}  

    	event.getLevel().destroyBlock(event.getPos(), true);
    	event.setCanceled(true);
  	
    	if (event.getLevel().getFluidState(event.getPos()).isEmpty()) {
    		return;
    	}
    	

    	int y = event.getPos().getY();
    	for (int i=0; i<16; i++ ) {
    		if (i+y > 127) {
     			break;
    		}
    		if (event.getLevel().getBlockState(event.getPos().above(i)).getLightEmission(event.getLevel(),event.getPos().above(i))<8) {
    			break;
    		}

    		if (event.getLevel().getFluidState(event.getPos().above(i)).isSource()) {
            	event.getLevel().setBlock(event.getPos().above(i), Blocks.COBBLESTONE.defaultBlockState(), 3);
    		} else {
            	event.getLevel().setBlock(event.getPos().above(i), Blocks.AIR.defaultBlockState(), 3);
    		}
    	}
	}
}
