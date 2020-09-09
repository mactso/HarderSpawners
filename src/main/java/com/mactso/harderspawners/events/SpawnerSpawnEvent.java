package com.mactso.harderspawners.events;

import java.util.Random;

import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager;
import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager.MobSpawnerBreakPercentageItem;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.events.SpawnerBreakEvent;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;

public class SpawnerSpawnEvent {
	private static int debugThreadIdentifier = 0;
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCheckSpawnerSpawn(LivingSpawnEvent.CheckSpawn event) {

//    	ASM code needed here:
//
//    	package net.minecraft.entity.monster;
//    	public static boolean canMonsterSpawnInLight(EntityType<? extends MonsterEntity> type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
//    	     return worldIn.getDifficulty() != Difficulty.PEACEFUL && isValidLightLevel(worldIn, pos, randomIn) && canSpawnOn(type, worldIn, reason, pos, randomIn);
//    	}
//
//      Look at beekeeper
//	    
    	
    	// context - this event only happens once every 15 to 45 seconds per active spawner.
    	
    	if (event.getSpawnReason() != SpawnReason.SPAWNER) {
    		return;
    	}

    	if (MyConfig.debugLevel > 0) {
        	debugThreadIdentifier = (debugThreadIdentifier + 1 ) % 10000;
        	System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Checking Spawner Spawn Event at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
    	}
    	if (!(event.getWorld() instanceof ServerWorld)) {
    		return;
    	}
    	
    	ServerWorld serverWorld = (ServerWorld) event.getWorld();
    	BlockPos eventPos = new BlockPos(event.getX(),event.getY(),event.getZ());
    	boolean azizLight = false;
    	if (event.getWorld().getLight(eventPos) > 6) {
        		removeLightNearSpawner(event, serverWorld);    			
    	}
    	// check that it's server world.
    	if (MyConfig.debugLevel > 0) {
        	System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Server World.   Lit:"+azizLight+ " at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
    	}
    	
    	boolean spawnOk = false;
    	

    	AbstractSpawner AbSp = event.getSpawner();

    	BlockPos AbSpPos = AbSp.getSpawnerPosition();

        LivingEntity le = (LivingEntity) event.getEntityLiving();
        String leStr = le.getType().getRegistryName().toString();
		MobSpawnerBreakPercentageItem t = MobSpawnerBreakPercentageItemManager.getMobSpawnerBreakPercentage(leStr);
	
		if (t == null) {
			leStr = "harderspawners:default";
			t = MobSpawnerBreakPercentageItemManager.getMobSpawnerBreakPercentage(leStr);
		}
		
		double mobSpawnerBreakPercentage = t.getSpawnerBreakPercentage();
		if (mobSpawnerBreakPercentage == 0.0) {
				return;
		} 

    	if (MyConfig.debugLevel > 0) {
    		System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Spawner failing at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
    	}
        Random chance = event.getWorld().getRandom();
        double next = 100.0 * chance.nextDouble();
        
        if (next < mobSpawnerBreakPercentage) {
        	next = 100.0 * chance.nextDouble();
        	if (next < MyConfig.spawnersExplodePercentage) {
            	if (MyConfig.debugLevel > 0) {
            		System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Spawner exploded at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
            	}
        		int flags = 3;  // Update Block- Tell Clients.
        		serverWorld.setBlockState(AbSpPos, Blocks.TNT.getDefaultState(),flags);
        		serverWorld.setBlockState(AbSpPos.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), flags);
        	} else {
            	if (MyConfig.debugLevel > 0) {
            		System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Spawner poofed at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
            	}
            	serverWorld.destroyBlock(AbSpPos, false);
        	}
        }
    }
    
	public void removeLightNearSpawner(LivingSpawnEvent.CheckSpawn event, ServerWorld serverWorld) {
		int fX = (int) event.getX();
		int fZ = (int) event.getZ();
		int fYmin = (int) event.getY()-4;
		if (fYmin < 1) fYmin = 1;
		int fYmax = (int) event.getY()+4;
		if (fYmax > 254) fYmin = 254;
		int scanSize = 7;
		for (int dy = fYmin; dy<= fYmax; dy++) {
			for (int dx = fX - scanSize; dx <= fX+scanSize; dx++) {
				for (int dz = fZ - scanSize; dz <= fZ+scanSize; dz++) {
					BlockPos bP = new BlockPos(dx,dy,dz);
					Block b = serverWorld.getBlockState(bP).getBlock();
					int blockLightLevel = serverWorld.getBlockState(bP).getLightValue();
			    	if ((blockLightLevel > 7)) {
			    		serverWorld.destroyBlock(bP, true);
			    	} 
					if (b == Blocks.LAVA) {
						serverWorld.setBlockState(bP, Blocks.AIR.getDefaultState(), 3);
					}
				}
			}
		}
	}
}
