
package com.mactso.harderspawners;

import com.mactso.harderspawners.commands.HarderSpawnersCommands;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.events.ServerTickHandler;
import com.mactso.harderspawners.sounds.ModSounds;
import com.mactso.harderspawners.util.MyUtilities;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod("harderspawners")
public class Main {

    public static final String MODID = "harderspawners";

    public Main(FMLJavaModLoadingContext context) {
        // Register config
        context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);

        MyUtilities.debugMsg(0, MODID + ": Registering Mod.");
    }


    // Static class for mod-related events on the Forge event bus
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onRegisterCommands(final RegisterCommandsEvent event) {
            MyUtilities.debugMsg(0, "HarderSpawners: Registering Commands");
            HarderSpawnersCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerStopping(final ServerStoppingEvent event) {
            ServerTickHandler.resetShutdown();
        }

        @SubscribeEvent
        public static void handleRegisterEvent(final RegisterEvent event) {
            ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
            if (key.equals(ForgeRegistries.Keys.SOUND_EVENTS)) {
                ModSounds.register(event.getForgeRegistry());
            }
        }
    }

}
