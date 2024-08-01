package com.mactso.harderspawners.config;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.Main;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)

public class MyConfig {

	public static class Common {

		public final IntValue debugLevel;
		public final IntValue spawnerTextOff;
		public final IntValue spawnerMinutesStunned;
		public final IntValue destroyLightPercentage;
		public final IntValue destroyLightRange;
		public final IntValue spawnerBreakSpeedMultiplier;
		public final IntValue spawnerRevengeLevel;
		public final IntValue requiredPlayerRange;
		public final IntValue maxNearbyEntities;
		public final IntValue spawnRange;
		public final IntValue hostileSpawnerLightLevel;
		public final IntValue hostileSpawnerResistDaylightDuration;
		public final DoubleValue spawnersExplodePercentage;

		public final ConfigValue<String> defaultNoBreakMobsActual;
		public final ConfigValue<String> defaultLightBlocksActual;

		public final String defaultNoBreakMobs6464 = "harderspawners:default,0.2;" + "minecraft:pig,0.0;"
				+ "minecraft:cow,0.0;" + "minecraft:sheep,0.0;" + "minecraft:parrot,0.0;" + "minecraft:blaze,0.0;";
		public final String defaultLightBlocks6464 = 
				"minecraft:redstone_lamp;" 
				+ "create:rose_quartz_lamp;";
		
		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("Harder Spawners Control Values");

			debugLevel = builder.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(Main.MODID + ".config." + "debugLevel").defineInRange("debugLevel", () -> 0, 0, 2);

			spawnerTextOff = builder.comment("0 = chat messages on, 1 = chat messages off.")
					.translation(Main.MODID + ".config." + "spawnerTextOff")
					.defineInRange("spawnerTextOff", () -> 1, 0, 1);

			destroyLightPercentage = builder.comment("Chance to destroy light sources in range (0-100%)")
					.translation(Main.MODID + ".config." + "destroyLightPercentage")
					.defineInRange("destroyLightPercentage", () -> 100, 0, 100);

			destroyLightRange = builder.comment("Range of light source destruction in blocks (1-7)")
					.translation(Main.MODID + ".config." + "destroyLightRange")
					.defineInRange("destroyLightRange", () -> 7, 1, 7);

			spawnerMinutesStunned = builder.comment("0- spawner breaks as normal.  Values over 0 minutes stun the spawner but don't break it.")
					.translation(Main.MODID + ".config." + "spawnerMinutesStunned")
					.defineInRange("spawnerMinutesStunned", () -> 0, 0, 27);
			
			spawnerBreakSpeedMultiplier = builder
					.comment("Spawner Break Speed Modifier: 0 = Off, 1 = 50% slower, From 2 to 2.1 billion times slower")
					.translation(Main.MODID + ".config." + "spawnerBreakSpeedMultiplier")
					.defineInRange("spawnerBreakSpeedMultiplier", () -> 4, 0, Integer.MAX_VALUE);

			spawnerRevengeLevel = builder
					.comment("Spawner Revenge Level: 0 = Off, Over 1 spawner takes revenge on player.")
					.translation(Main.MODID + ".config." + "spawnerRevengeLevel")
					.defineInRange("spawnerRevengeLevel", () -> 1, 0, 11);

			spawnersExplodePercentage = builder.comment("Explode percentage when Spawners Break")
					.translation(Main.MODID + ".config." + "spawnersExplodePercentage")
					.defineInRange("spawnersExplodePercentage", () -> 33.0, 0.0, 100.0);

			requiredPlayerRange = builder.comment("Hostile Spawners: Player Range when hostile mobs start spawning")
					.translation(Main.MODID + ".config." + "requiredPlayerRange")
					.defineInRange("requiredPlayerRange", () -> 13, 7, 256);

			maxNearbyEntities = builder.comment("Hostile Spawners: Maximum Spawned Hostile Entities")
					.translation(Main.MODID + ".config." + "maxNearbyEntities")
					.defineInRange("maxNearbyEntities", () -> 9, 3, 256);

			spawnRange = builder.comment("Hostile Spawners: How far from spawner hostile mobs can spawn")
					.translation(Main.MODID + ".config." + "spawnRange").defineInRange("spawnRange", () -> 9, 4, 256);

			hostileSpawnerLightLevel = builder
					.comment("hostileSpawnerLightLevel: Maximum Light Level for Hostile Spawners")
					.translation(Main.MODID + ".config." + "hostileSpawnerLightLevel")
					.defineInRange("hostileSpawnerLightLevel", () -> 11, 0, 15);

			hostileSpawnerResistDaylightDuration = builder
					.comment("hostileSpawnerResistDaylightDuration: give undead fire resistance (true) ")
					.translation(Main.MODID + ".config." + "hostileSpawnerResistDaylightDuration")
					.defineInRange("hostileSpawnerResistDaylightDuration", () -> 120, 0, 9999);

			builder.pop();
			
			builder.push("List of modded lamp and light blocks");
			defaultLightBlocksActual = builder.comment("List of modded lamp and light blocks")
					.translation(Main.MODID + ".config" + "defaultLightBlocksActual")
					.define("defaultLightBlocksActual", defaultLightBlocks6464);
			
			
			builder.pop();

			builder.push("No Break Mobs Values 6464");

			defaultNoBreakMobsActual = builder.comment("No Break Mob Values")
					.translation(Main.MODID + ".config" + "defaultNoBreakMobsActual")
					.define("defaultNoBreakMobsActual", defaultNoBreakMobs6464);

			builder.pop();
		}

	}
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;


	static {
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	private static int debugLevel;
	private static int spawnerTextOff;
	private static int spawnerMinutesStunned;
	private static int destroyLightPercentage;
	private static int destroyLightRange;
	private static int spawnerBreakSpeedMultiplier;
	private static int spawnerRevengeLevel;
	private static int maxNearbyEntities;
	private static int requiredPlayerRange;
	private static int spawnRange;
	private static int hostileSpawnerLightLevel;
	private static int hostileSpawnerResistDaylightDuration;
	private static double spawnersExplodePercentage;
	private static String[] defaultMobBreakPercentageValues;
	private static String defaultMobBreakPercentageValues6464;
	private static String[] defaultLightBlocksValues;
	private static String defaultLightBlocksValues6464;	
	
	public static void bakeConfig() {
		debugLevel = COMMON.debugLevel.get();
		setSpawnerTextOff(COMMON.spawnerTextOff.get());
		spawnerMinutesStunned = COMMON.spawnerMinutesStunned.get();
		setSpawnerBreakSpeedMultiplier(COMMON.spawnerBreakSpeedMultiplier.get());
		destroyLightPercentage = COMMON.destroyLightPercentage.get();
		destroyLightRange = COMMON.destroyLightRange.get();
		
		setSpawnersExplodePercentage(COMMON.spawnersExplodePercentage.get());
		setSpawnerRevengeLevel(COMMON.spawnerRevengeLevel.get());
		maxNearbyEntities = COMMON.maxNearbyEntities.get();
		requiredPlayerRange = COMMON.requiredPlayerRange.get();
		spawnRange = COMMON.spawnRange.get();
		hostileSpawnerLightLevel = COMMON.hostileSpawnerLightLevel.get();
		hostileSpawnerResistDaylightDuration = COMMON.hostileSpawnerResistDaylightDuration.get();
		setDefaultMobBreakPercentageValues6464(COMMON.defaultNoBreakMobsActual.get());
		setDefaultLightBlocksValues6464(COMMON.defaultLightBlocksActual.get());
		if (debugLevel > 0) {
			System.out.println("Harder Spawners Debug: " + debugLevel);
		}
	}
	public static int getDebugLevel() {
		return debugLevel;
	}

	public static int getDestroyLightPercentage() {
		return destroyLightPercentage;
	}
	
	public static int getDestroyLightRange() {
		return destroyLightRange;
	}
	public static int getHostileSpawnerLightLevel() {
		return hostileSpawnerLightLevel;
	}
	public static int getHostileSpawnerResistDaylightDuration() {
		return hostileSpawnerResistDaylightDuration;
	}
	public static int getMaxNearbyEntities() {
		return maxNearbyEntities;
	}

	public static int getRequiredPlayerRange() {
		return requiredPlayerRange;
	}

	public static int getSpawnerMinutesStunned() {
		return spawnerMinutesStunned;
	}

	public static int getSpawnRange() {
		return spawnRange;
	}

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent) {
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();
			MobSpawnerManager.mobBreakPercentageInit();
			//LightBlocksManager.lightBlocksInit();
		}
	}

	public static void pushDebugValue() {
		if (debugLevel > 0) {
			System.out.println("harderspawners debugLevel:" + MyConfig.debugLevel);
		}
		COMMON.debugLevel.set(MyConfig.debugLevel);
	}

	public static void pushSpawnerRevenge() {
		if (debugLevel > 0) {
			System.out.println("harderspawners: revengeLevel" + MyConfig.getSpawnerRevengeLevel());
		}
		COMMON.spawnerRevengeLevel.set(MyConfig.getSpawnerRevengeLevel());
	}

	public static void pushSpawnersExplodePercentage() {
		if (debugLevel > 0) {
			System.out.println("harderspawners: explode% :" + MyConfig.getSpawnersExplodePercentage());
		}
		COMMON.spawnersExplodePercentage.set(MyConfig.getSpawnersExplodePercentage());
	}

	public static void setDebugLevel(int debugLevel) {
		MyConfig.debugLevel = debugLevel;
	}
	
	public static int getSpawnerBreakSpeedMultiplier() {
		return spawnerBreakSpeedMultiplier;
	}
	
	public static void setSpawnerBreakSpeedMultiplier(int spawnerBreakSpeedMultiplier) {
		MyConfig.spawnerBreakSpeedMultiplier = spawnerBreakSpeedMultiplier;
	}
	
	public static int getSpawnerRevengeLevel() {
		return spawnerRevengeLevel;
	}
	
	public static void setSpawnerRevengeLevel(int spawnerRevengeLevel) {
		MyConfig.spawnerRevengeLevel = spawnerRevengeLevel;
	}
	
	public static int getSpawnerTextOff() {
		return spawnerTextOff;
	}
	
	public static void setSpawnerTextOff(int spawnerTextOff) {
		MyConfig.spawnerTextOff = spawnerTextOff;
	}
	
	public static String getDefaultMobBreakPercentageValues6464() {
		return defaultMobBreakPercentageValues6464;
	}
	
	public static void setDefaultMobBreakPercentageValues6464(String defaultMobBreakPercentageValues6464) {
		MyConfig.defaultMobBreakPercentageValues6464 = defaultMobBreakPercentageValues6464;
	}

	public static void setDefaultLightBlocksValues6464(String defaultLightBlocksValues6464) {
		MyConfig.defaultLightBlocksValues6464 = defaultLightBlocksValues6464;
	}
	
	public static String[] getDefaultMobBreakPercentageValues() {
		return defaultMobBreakPercentageValues;
	}

	public static void setDefaultMobBreakPercentageValues(String[] s) {
		MyConfig.defaultMobBreakPercentageValues = s;
	}

	
	public static String getDefaultLightBlocksValues6464() {
		return defaultLightBlocksValues6464 ;
	}
	
	public static String[] getDefaultLightBlocksValues() {
		return defaultLightBlocksValues;
	}

	public static void setDefaultLightBlocksValues(String[] s) {
		MyConfig.defaultLightBlocksValues = s;
	}

	public static double getSpawnersExplodePercentage() {
		return spawnersExplodePercentage;
	}
	
	public static void setSpawnersExplodePercentage(double spawnersExplodePercentage) {
		MyConfig.spawnersExplodePercentage = spawnersExplodePercentage;
	}

}
