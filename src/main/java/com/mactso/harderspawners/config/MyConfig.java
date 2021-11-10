package com.mactso.harderspawners.config;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.Main;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

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
		public static int spawnerTextOff;
		public static int destroyLightPercentage;
		public static int destroyLightRange;
		public static int spawnerBreakSpeedMultiplier;
		public static int spawnerRevengeLevel;
		public static double spawnersExplodePercentage;
		public static String[]  defaultMobBreakPercentageValues;
		public static String    defaultMobBreakPercentageValues6464;

		public static int getDestroyLightPercentage() {
			return destroyLightPercentage;
		}

		public static int getDestroyLightRange() {
			return destroyLightRange;
		}
		
		@SubscribeEvent
		public static void onModConfigEvent(final ModConfigEvent configEvent)
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
			spawnerTextOff = COMMON.spawnerTextOff.get();
			spawnerBreakSpeedMultiplier = COMMON.spawnerBreakSpeedMultiplier.get();
			destroyLightPercentage = COMMON.destroyLightPercentage.get();
			destroyLightRange = COMMON.destroyLightRange.get();
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
			public final IntValue spawnerTextOff;
			public final IntValue destroyLightPercentage;
			public final IntValue destroyLightRange;
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

				spawnerTextOff = builder
						.comment("0 = chat messages on, 1 = chat messages off.")
						.translation(Main.MODID + ".config." + "spawnerTextOff")
						.defineInRange("spawnerTextOff", () -> 1, 0, 1);
				
				destroyLightPercentage = builder
						.comment("Chance to destroy light sources in range (0-100%)")
						.translation(Main.MODID + ".config." + "destroyLightPercentage")
						.defineInRange("destroyLightPercentage", () -> 100, 0, 100);

				destroyLightRange = builder
						.comment("Range of light source destruction in blocks (1-7)")
						.translation(Main.MODID + ".config." + "destroyLightRange")
						.defineInRange("destroyLightRange", () -> 7, 1, 7);

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
		public static void sendChat(Player p, String chatMessage, TextColor color) {
			TextComponent component = new TextComponent (chatMessage);
			component.getStyle().withColor(color);
			p.sendMessage(component, p.getUUID());
		}
		
		// support for any color, optionally bold text.
		public static void sendBoldChat(Player p, String chatMessage, TextColor color) {
			TextComponent component = new TextComponent (chatMessage);

			component.getStyle().withBold(true);
			component.getStyle().withColor(color);
			p.sendMessage(component, p.getUUID());
		}	
}
