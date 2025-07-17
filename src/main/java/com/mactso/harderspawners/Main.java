
package com.mactso.harderspawners;

import org.jetbrains.annotations.NotNull;

import com.mactso.harderspawners.commands.HarderSpawnersCommands;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.events.ServerTickHandler;
import com.mactso.harderspawners.sounds.ModSounds;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod("harderspawners")
public class Main {

	public static final String MODID = "harderspawners";

    public Main(FMLJavaModLoadingContext context)
    {
		// context.getModEventBus().register(this);  Use when registering several methods.
		context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::handleCommonSetupEvent);


        Utility.debugMsg(0,MODID + ": Registering Mod.");
	}
    
    // Register ourselves for server and other game events we are interested in
	@SubscribeEvent
	public void handleCommonSetupEvent(final FMLCommonSetupEvent event) {
		Utility.debugMsg(0, "harderspawners: Registering Handlers.");
		// Add Networking lines here if the mod has them.
		// MinecraftForge.EVENT_BUS.register(new SbeAttachEvent());  remove here and make it static
		// MinecraftForge.EVENT_BUS.register(new SpawnerBreakHandler());
		// MinecraftForge.EVENT_BUS.register(new SpawnerSpawnEvent());
		// MinecraftForge.EVENT_BUS.register(new SpawnerLightOnTopEvent());
		// MinecraftForge.EVENT_BUS.register(new MyEntityPlaceEvent());
		// MinecraftForge.EVENT_BUS.register(new ServerTickHandler());

	}

	@Mod.EventBusSubscriber()
	public static class ModEvents {
		@SubscribeEvent
		public static void handleRegisterEventSetup(final RegisterEvent event) {
//	dbg		@Nullable
//	dbg		IForgeRegistry<Object> fr = event.getForgeRegistry();

			@NotNull
			ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
			if (key.equals(ForgeRegistries.Keys.SOUND_EVENTS)) {
				ModSounds.register(event.getForgeRegistry());
			}
		}

		@Mod.EventBusSubscriber()
		public static class ForgeEvents {

			@SubscribeEvent 		
			public static void onCommandsRegistry(final RegisterCommandsEvent event) {
				Utility.debugMsg(0,"HarderBranchMining: Registering Commands");
				HarderSpawnersCommands.register(event.getDispatcher());			
			}
			
			@SubscribeEvent
			public static void onServerStopping(ServerStoppingEvent event) {
				ServerTickHandler.resetShutdown();
			}

		}
	}

}
