package com.mactso.harderspawners.events;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerLightOnTopEvent {
	@SubscribeEvent
	public void onNeighborNotifyEvent (NeighborNotifyEvent event) {

		if (!(event.getNotifiedSides().contains(Direction.DOWN))) {
			return;
		}
		
    	if (event.getWorld().getBlockState(event.getPos().below()).getBlock() != Blocks.SPAWNER) {
    		return;
    	};
    	
		BlockState bS = event.getState();
    	if ((bS.getLightEmission() < 8)&&(bS.getBlock() != Blocks.REDSTONE_LAMP)) {
    		return;
    	}  

    	event.getWorld().destroyBlock(event.getPos(), true);
    	event.setCanceled(true);
  	
    	if (event.getWorld().getFluidState(event.getPos()).isEmpty()) {
    		return;
    	}
    	
    	boolean whileGlowingFluid = true;
    	int y = event.getPos().getY();
    	for (int i=0; i<16; i++ ) {
    		if (i+y > 127) {
    			whileGlowingFluid= false;
    			break;
    		}
    		if (event.getWorld().getBlockState(event.getPos().above(i)).getLightEmission()<8) {
    			whileGlowingFluid= false;
    			break;
    		}

    		if (event.getWorld().getFluidState(event.getPos().above(i)).isSource()) {
            	event.getWorld().setBlock(event.getPos().above(i), Blocks.COBBLESTONE.defaultBlockState(), 3);
    		} else {
            	event.getWorld().setBlock(event.getPos().above(i), Blocks.AIR.defaultBlockState(), 3);
    		}
    	}
	}
}
