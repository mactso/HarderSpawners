
package com.mactso.harderspawners;


import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.events.MyEntityPlaceEvent;
import com.mactso.harderspawners.events.SbeAttachEvent;
import com.mactso.harderspawners.events.ServerTickHandler;
import com.mactso.harderspawners.events.SpawnerBreakHandler;
import com.mactso.harderspawners.events.SpawnerLightOnTopEvent;
import com.mactso.harderspawners.events.SpawnerSpawnEvent;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("harderspawners")
public class Main {

	    public static final String MODID = "harderspawners"; 
	    
	    public Main()
	    {
			System.out.println("harderspawners: Registering Mod.");
			FMLJavaModLoadingContext.get().getModEventBus().register(this);
	        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,MyConfig.COMMON_SPEC );
	    }


		@SubscribeEvent 
		public void preInit (final FMLCommonSetupEvent event) {
				System.out.println("harderspawners: Registering Handler");
				MinecraftForge.EVENT_BUS.register(new SbeAttachEvent());
				MinecraftForge.EVENT_BUS.register(new SpawnerBreakHandler ());
				MinecraftForge.EVENT_BUS.register(new SpawnerSpawnEvent());
				MinecraftForge.EVENT_BUS.register(new SpawnerLightOnTopEvent());
				MinecraftForge.EVENT_BUS.register(new MyEntityPlaceEvent());
				MinecraftForge.EVENT_BUS.register(new ServerTickHandler());
		}   
		
		

}
