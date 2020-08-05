package com.mactso.harderspawners.events;

import java.util.Random;

import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager;
import com.mactso.harderspawners.config.MobSpawnerBreakPercentageItemManager.MobSpawnerBreakPercentageItem;
import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class SpawnerSpawnEvent {

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
    	if (event.getSpawnReason() != SpawnReason.SPAWNER) {
    		return;
    	}
    	
    	IWorld iWorld = event.getWorld();
    	// check that it's server world.
    	
    	System.out.println ("HarderSpawners: Checked Spawner Spawn Event.");
    	boolean spawnOk = false;

    	AbstractSpawner AbSp = event.getSpawner();

    	BlockPos AbSpPos = AbSp.getSpawnerPosition();

    	System.out.println ("HarderSpawners: Event has a spawner.");

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
    
        System.out.println ("HarderSpawners: Break / Explode Check.");

        Random chance = iWorld.getRandom();
        double next = 100.0 * chance.nextDouble();
        
        if (next < mobSpawnerBreakPercentage) {
        	next = 100.0 * chance.nextDouble();
        	if (next < MyConfig.spawnersExplodePercentage) {
        		int flags = 3;  // Update Block- Tell Clients.
        		iWorld.setBlockState(AbSpPos, Blocks.TNT.getDefaultState(),flags);
        		iWorld.setBlockState(AbSpPos.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), flags);
        	} else {
        		iWorld.destroyBlock(AbSpPos, false);
        	}
        }
    }
}
