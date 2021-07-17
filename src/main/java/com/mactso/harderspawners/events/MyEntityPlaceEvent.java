package com.mactso.harderspawners.events;


import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MyEntityPlaceEvent {

    @SubscribeEvent()
    public void bucket(FillBucketEvent event)  {
    	World world = (World) event.getWorld();
    	ItemStack stack = event.getEmptyBucket();
    	BlockPos pos = null;
    	if (event.getTarget().getType() == Type.BLOCK) {
    		BlockRayTraceResult br = (BlockRayTraceResult) event.getTarget();
    		pos = br.getBlockPos().relative(br.getDirection());
        	if (pos != null && stack.getItem() instanceof BucketItem) {
        		BucketItem b = (BucketItem) stack.getItem();
    	        	if (isSpawnerNearby(world, pos)) {
    	        		if (b.getFluid().getAttributes().getLuminosity() > 8) {
    	        			if (!world.isClientSide()) {
    	    					world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH,
    	    							SoundCategory.AMBIENT, 0.9f, 0.25f);    	        				
    	        			}
    	        			event.setCanceled(true);
    	        	}
        		}
        	}
    	}
   }
	
	
    @SubscribeEvent()
    public void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
    	World world = (World) event.getWorld();
    	BlockState placedBlockState = event.getPlacedBlock();
    	BlockPos placedBlockPos = event.getPos();
    	Block placedBlock = placedBlockState.getBlock();
    	
    	int blockLightValue = placedBlockState.getLightEmission();
    	if (placedBlock == Blocks.REDSTONE_LAMP) {
    		blockLightValue = 15;
    	}  
    	
    	if (isSpawnerNearby(world, placedBlockPos) &&
    			world.getMaxLocalRawBrightness(placedBlockPos) < 15 &&
    			blockLightValue > 8)  {
    		if (world.getRandom().nextInt(100) <= MyConfig.getDestroyLightPercentage()) {
        		world.destroyBlock(placedBlockPos, true);
    		}
    	}
    }
    
    private boolean isSpawnerNearby (World world, BlockPos blockPos) {

    	int x = blockPos.getX();
    	int y = blockPos.getY();
    	int z = blockPos.getZ();
    	int destroyRange = MyConfig.getDestroyLightRange();
    	int destroyYRange = destroyRange/2;
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
    	for ( dy=(0-destroyYRange);dy<(int)destroyYRange;dy++) {
			for( dx=(0-destroyRange);dx<(0+destroyRange);dx++) {
				for( dz=(0-destroyRange);dz<(0+destroyRange);dz++) {
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
