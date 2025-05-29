
package com.mactso.harderspawners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.events.MyEntityPlaceEvent;
import com.mactso.harderspawners.events.SbeAttachEvent;
import com.mactso.harderspawners.events.ServerTickHandler;
import com.mactso.harderspawners.events.SpawnerBreakHandler;
import com.mactso.harderspawners.events.SpawnerLightOnTopEvent;
import com.mactso.harderspawners.events.SpawnerSpawnEvent;
import com.mactso.harderspawners.sounds.ModSounds;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

@Mod("harderspawners")
public class Main {

	public static final String MODID = "harderspawners";

	public Main(FMLJavaModLoadingContext context) {
		context.getModEventBus().register(this);
		context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
		Utility.debugMsg(0, MODID + ": Registering Mod");
	}

	@SubscribeEvent
	public void preInit(final FMLCommonSetupEvent event) {
		Utility.debugMsg(0, "harderspawners: Registering Handlers.");
		MinecraftForge.EVENT_BUS.register(new SbeAttachEvent());
		MinecraftForge.EVENT_BUS.register(new SpawnerBreakHandler());
		MinecraftForge.EVENT_BUS.register(new SpawnerSpawnEvent());
		MinecraftForge.EVENT_BUS.register(new SpawnerLightOnTopEvent());
		MinecraftForge.EVENT_BUS.register(new MyEntityPlaceEvent());
		MinecraftForge.EVENT_BUS.register(new ServerTickHandler());

	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModEvents {
		@SubscribeEvent
		public static void onRegister(final RegisterEvent event) {
			@Nullable
			IForgeRegistry<Object> fr = event.getForgeRegistry();

			@NotNull
			ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
			if (key.equals(ForgeRegistries.Keys.SOUND_EVENTS)) {
				ModSounds.register(event.getForgeRegistry());
			}
		}

		@Mod.EventBusSubscriber(bus = Bus.FORGE)
		public static class ForgeEvents {

			@SubscribeEvent
			public static void onServerStopping(ServerStoppingEvent event) {
				ServerTickHandler.resetShutdown();
			}

		}
	}

}
