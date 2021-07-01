package com.mactso.harderspawners.events;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerLightOnTopEvent {
	@SubscribeEvent
	public void onNeighborNotifyEvent (NeighborNotifyEvent event) {

		if (!(event.getNotifiedSides().contains(Direction.DOWN))) {
			return;
		}
		
    	if (event.getWorld().getBlockState(event.getPos().down()).getBlock() != Blocks.SPAWNER) {
    		return;
    	};
    	
		BlockState bS = event.getState();
    	if ((bS.getLightValue() < 8)&&(bS.getBlock() != Blocks.REDSTONE_LAMP)) {
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
    		if (event.getWorld().getBlockState(event.getPos().up(i)).getLightValue()<8) {
    			whileGlowingFluid= false;
    			break;
    		}

    		if (event.getWorld().getFluidState(event.getPos().up(i)).isSource()) {
            	event.getWorld().setBlockState(event.getPos().up(i), Blocks.COBBLESTONE.getDefaultState(), 3);
    		} else {
            	event.getWorld().setBlockState(event.getPos().up(i), Blocks.AIR.getDefaultState(), 3);
    		}
    	}
	}
}
