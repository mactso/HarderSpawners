package com.mactso.harderspawners.events;

import java.util.Random;

import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager;
import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager.MobSpawnerBreakPercentageItem;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    	if (event.getSpawnReason() != MobSpawnType.SPAWNER) {
    		return;
    	}

    	
    	if (MyConfig.debugLevel > 0) {
        	debugThreadIdentifier = (debugThreadIdentifier + 1 ) % 10000;
        	System.out.println ("HarderSpawners: ("+debugThreadIdentifier+") Checking Spawner Spawn Event at "+(int)event.getX()+"+(int)event.getY()+"+(int)event.getZ()+".");
    	}
    	if (!(event.getWorld() instanceof ServerLevel)) {
    		return;
    	}
    	
    	ServerLevel serverWorld = (ServerLevel) event.getWorld();
    	long gametime = serverWorld.getGameTime();

    	BlockPos eventPos = new BlockPos(event.getX(),event.getY(),event.getZ());

    	if (serverWorld.getMaxLocalRawBrightness(eventPos) > 6) {
        		boolean destroyedLight = 
        				SharedUtilityMethods.removeLightNearSpawner(eventPos, serverWorld);    			
    	}
    	

   	
    	BlockPos AbSpPos = event.getSpawner().getSpawnerBlockEntity().getBlockPos();

        LivingEntity le = (LivingEntity) event.getEntityLiving();
        String leStr = le.getType().getRegistryName().toString();
		MobSpawnerBreakPercentageItem t = MobSpawnerBreakPercentageItemManager.getMobSpawnerBreakPercentage(leStr);
	
		if (t == null) {
			leStr = "harderspawners:default";
			t = MobSpawnerBreakPercentageItemManager.getMobSpawnerBreakPercentage(leStr);
		}
		
		double mobSpawnerBreakPercentage = 0.2; // 0.2%
		if (t != null) {
			mobSpawnerBreakPercentage = t.getSpawnerBreakPercentage();
		} 
		
		if (mobSpawnerBreakPercentage == 0.0) {
				return;
		} 

		mobSpawnerBreakPercentage = mobSpawnerBreakPercentage / 4; // called 4 times in tick it spawns.
		
        Random chance = event.getWorld().getRandom();
        double failRoll = 100.0 * chance.nextDouble();
        boolean canExplode = true;
        // keep in mind default odds are 1/500 (so 0.2 is 0.2% chance, not a 20%).

        if (failRoll < mobSpawnerBreakPercentage) {
        	double explodeRoll = 100.0 * chance.nextDouble();
        	if (le instanceof Silverfish) {
        		canExplode = false;
        	}
        	if ((canExplode) && (explodeRoll < MyConfig.spawnersExplodePercentage)) {
        		int flags = 3;  // Update Block- Tell Clients.
        		serverWorld.setBlock(AbSpPos, Blocks.TNT.defaultBlockState(),flags);
        		serverWorld.setBlock(AbSpPos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), flags);
        	} else {
            	serverWorld.destroyBlock(AbSpPos, false);
        	}
        }
    }
   

}
