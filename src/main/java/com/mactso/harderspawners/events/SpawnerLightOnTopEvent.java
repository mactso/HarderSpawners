package com.mactso.harderspawners.events;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.core.Direction;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber() 
public class SpawnerLightOnTopEvent {
	@SubscribeEvent
	public static boolean onNeighborNotifyEvent(BlockEvent.NeighborNotifyEvent event) {
		
		boolean result = MyConfig.CONTINUE_EVENT;

		if (!(event.getNotifiedSides().contains(Direction.DOWN))) {
			return result;
		}
		
    	if (event.getLevel().getBlockState(event.getPos().below()).getBlock() != Blocks.SPAWNER) {
    		return result;
    	}
    	
		BlockState bS = event.getState();
    	if ((bS.getLightEmission(event.getLevel(), event.getPos().below()) < 8)&&(bS.getBlock() != Blocks.REDSTONE_LAMP)) {
    		return result;
    	}  

    	event.getLevel().destroyBlock(event.getPos(), true);
    	result = MyConfig.CANCEL_EVENT;
  	
    	if (event.getLevel().getFluidState(event.getPos()).isEmpty()) {
    		return result;
    	}


		int y = event.getPos().getY();
		for (int i = 0; i < 16; i++) {
			if (i + y > 127) {
				break;
			}
			if (event.getLevel().getBlockState(event.getPos().above(i)).getLightEmission(event.getLevel(),
					event.getPos().above(i)) < 8) {
				break;
			}

			if (event.getLevel().getFluidState(event.getPos().above(i)).isSource()) {
				event.getLevel().setBlock(event.getPos().above(i), Blocks.COBBLESTONE.defaultBlockState(), 3);
			} else {
				event.getLevel().setBlock(event.getPos().above(i), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		
		return result;
	}
}
