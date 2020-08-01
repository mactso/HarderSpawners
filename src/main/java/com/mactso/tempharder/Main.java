// 15.2 - 1.0.0.0 Villager Respawn
package com.mactso.spawnerinlight;


import com.mactso.spawnerinlight.config.MyConfig;
import com.mactso.spawnerinlight.events.MyPlaceEntityEvent;
import com.mactso.spawnerinlight.events.SpawnerSpawnEvent;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("harderspawners")
public class Main {

	    public static final String MODID = "harderspawners"; 
	    
	    public Main()
	    {
			System.out.println("harderspawners: Registering Mod.");
			FMLJavaModLoadingContext.get().getModEventBus().register(this);
	        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,MyConfig.SERVER_SPEC );
	    }


		@SubscribeEvent 
		public void preInit (final FMLCommonSetupEvent event) {
				System.out.println("harderspawners: Registering Handler");
				MinecraftForge.EVENT_BUS.register(new SpawnerSpawnEvent());
				MinecraftForge.EVENT_BUS.register(new MyPlaceEntityEvent());
		}   
		
		

}
