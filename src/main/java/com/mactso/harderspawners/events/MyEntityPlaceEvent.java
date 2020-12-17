package com.mactso.harderspawners.events;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MyEntityPlaceEvent {

    @SubscribeEvent()
    public void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
    	Entity entityPlacingBlock = event.getEntity();

    	if (entityPlacingBlock.world.isRemote()) {
    		return;
    	}
    	World world = entityPlacingBlock.world;
    	

    	// is player available?
    	
    	String name = entityPlacingBlock.getCachedUniqueIdString();
    	BlockState placedBlockState = event.getPlacedBlock();
    	int blockLightValue = placedBlockState.getLightValue();
    	BlockPos placedBlockPos = event.getPos();
    	Block placedBlock = placedBlockState.getBlock();
    	
//    	// okay to place lights outside .
//    	if (iWorld.canSeeSky(placedBlockPos)) {
//    		return;
//    	}

    	if ((blockLightValue < 8)&&(placedBlock != Blocks.REDSTONE_LAMP)) {
    		return;
    	}  
    	
    	// okay to place lights in already bright areas.
    	int light = world.getLight(placedBlockPos);

    	if (light==15) {
    		return;
    	}
    	
    	// placing dark light that's not a redstone lamp.
  	
    	if (isSpawnerNearby(world, placedBlockPos)) {
    		event.setCanceled(true);
    	}
   	
  	
    }
    
    private boolean isSpawnerNearby (World world, BlockPos blockPos) {

    	int x = blockPos.getX();
    	int y = blockPos.getY();
    	int z = blockPos.getZ();
    	int dx, dy, dz;
    	// quick scan for spawner
    	
		for ( dy=-1;dy<2;dy++) {
			for( dx=-1;dx<2;dx++) {
				for( dz=-1;dz<2;dz++) {
					Block tempBlock = world.getBlockState(new BlockPos (x+dx,y+dy,z+dz)).getBlock();
					if (tempBlock == Blocks.SPAWNER) {
						return true;
					}
				}
			}
		}
		// slower scan for spawner
    	for ( dy=-1;dy<3;dy++) {
			for( dx=-6;dx<6;dx++) {
				for( dz=-6;dz<6;dz++) {
					Block tempBlock = world.getBlockState(new BlockPos (x+dx,y+dy,z+dz)).getBlock();
					if (tempBlock == Blocks.SPAWNER) {
						return true;
					}
				}
			}
		}
    	
    	return false;
    }
} 
