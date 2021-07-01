package com.mactso.harderspawners.config;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.Main;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)

public class MyConfig
{
		private static final Logger LOGGER = LogManager.getLogger();
		public static final Common COMMON;
		public static final ForgeConfigSpec COMMON_SPEC;
		static
		{
			final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
			COMMON_SPEC = specPair.getRight();
			COMMON = specPair.getLeft();
		}

		public static int debugLevel;
		public static int spawnerBreakSpeedMultiplier;
		public static int spawnerRevengeLevel;
		public static double spawnersExplodePercentage;
		public static String[]  defaultMobBreakPercentageValues;
		public static String    defaultMobBreakPercentageValues6464;
		
		@SubscribeEvent
		public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
		{
			if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC)
			{
				bakeConfig();
				MobSpawnerBreakPercentageItemManager.mobBreakPercentageInit();
			}
		}

		public static void pushDebugValue() {
			if (debugLevel > 0) {
				System.out.println("harderspawners debugLevel:"+MyConfig.debugLevel);
			}
			COMMON.debugLevel.set( MyConfig.debugLevel);
		}

		public static void pushSpawnersExplodePercentage() {
			if (debugLevel > 0) {
				System.out.println("harderspawners: explode% :"+MyConfig.spawnersExplodePercentage);
			}
			COMMON.spawnersExplodePercentage.set( MyConfig.spawnersExplodePercentage);
		}

		public static void pushSpawnerRevenge() {
			if (debugLevel > 0) {
				System.out.println("harderspawners: revengeLevel"+MyConfig.spawnerRevengeLevel);
			}
			COMMON.spawnerRevengeLevel.set( MyConfig.spawnerRevengeLevel);
		}
		
		public static void bakeConfig()
		{
			debugLevel = COMMON.debugLevel.get();
			spawnerBreakSpeedMultiplier = COMMON.spawnerBreakSpeedMultiplier.get();
			spawnersExplodePercentage = COMMON.spawnersExplodePercentage.get();
			spawnerRevengeLevel = COMMON.spawnerRevengeLevel.get();
			defaultMobBreakPercentageValues6464 = COMMON.defaultNoBreakMobsActual.get() ;
			if (debugLevel > 0) {
				System.out.println("Harder Spawners Debug: " + debugLevel );
			}
		}

		public static class Common
		{

			public final IntValue debugLevel;
			public final IntValue spawnerBreakSpeedMultiplier;
			public final IntValue spawnerRevengeLevel;
			public final DoubleValue spawnersExplodePercentage;
			
			public final ConfigValue<String> defaultNoBreakMobsActual;
			public final String defaultNoBreakMobs6464 = 
					  "harderspawners:default,0.2;"
					+ "minecraft:pig,0.0;"
					+ "minecraft:cow,0.0;"
					+ "minecraft:sheep,0.0;"
					+ "minecraft:parrot,0.0;"
					+ "minecraft:blaze,0.0;"
					;	
			
			public Common(ForgeConfigSpec.Builder builder)
			{
				builder.push("Spawners Spawn in Light Control Values");

				debugLevel = builder
						.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
						.translation(Main.MODID + ".config." + "debugLevel")
						.defineInRange("debugLevel", () -> 0, 0, 2);
			
				spawnerBreakSpeedMultiplier = builder
						.comment("Spawner Break Speed Modifier: 0 = Off, 1 = 50% slower, 2-11 times slower")
						.translation(Main.MODID + ".config." + "spawnerBreakSpeedMultiplier")
						.defineInRange("spawnerBreakSpeedMultiplier", () -> 4, 0, 11);

				spawnerRevengeLevel = builder
						.comment("Spawner Revenge Level: 0 = Off, Over 1 spawner takes revenge on player.")
						.translation(Main.MODID + ".config." + "spawnerRevengeLevel")
						.defineInRange("spawnerRevengeLevel", () -> 1, 0, 11);
				
				spawnersExplodePercentage = builder
						.comment("Explode percentage when Spawners Break")
						.translation(Main.MODID + ".config." + "spawnersExplodePercentage")
						.defineInRange("spawnersExplodePercentage", () -> 33.0, 0.0, 100.0);
				
				builder.pop();
				
				builder.push ("No Break Mobs Values 6464");
				
				defaultNoBreakMobsActual = builder
						.comment("Trail Block String 6464")
						.translation(Main.MODID + ".config" + "defaultNoBreakMobsActual")
						.define("defaultNoBreakMobsActual", defaultNoBreakMobs6464);
				builder.pop();	
			}
		}	
		
		// support for any color chattext
		public static void sendChat(PlayerEntity p, String chatMessage, Color color) {
			StringTextComponent component = new StringTextComponent (chatMessage);
			component.getStyle().setColor(color);
			p.sendMessage(component, p.getUniqueID());
		}
		
		// support for any color, optionally bold text.
		public static void sendBoldChat(PlayerEntity p, String chatMessage, Color color) {
			StringTextComponent component = new StringTextComponent (chatMessage);

			component.getStyle().setBold(true);
			component.getStyle().setColor(color);
			p.sendMessage(component, p.getUniqueID());
		}	
}
