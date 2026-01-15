
package com.mactso.harderspawners.modloader.config;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.main.Main;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

@EventBusSubscriber(modid = Main.MODID)
public class MyConfig {


	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent) {
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();

		}
	}
	
	public static class Common {

		public final IntValue debugLevel;
		public final IntValue spawnerTextOff;
		public final IntValue spawnerMinutesStunned;
		public final IntValue destroyLightPercentage;
		public final IntValue destroyLightRange;
		public final IntValue spawnerBreakSpeedModifier;
		public final IntValue spawnerRevengeLevel;
		public final IntValue requiredPlayerRange;
		public final IntValue maxNearbyEntities;
		public final IntValue spawnRange;
		public final IntValue hostileSpawnerLightLevel;
		public final IntValue hostileSpawnerResistDaylightDuration;
		public final DoubleValue spawnersExplodePercentage;

		public final ConfigValue<String> timeExtensionItem;
		public final IntValue spawnsAmount;
		public final ConfigValue<String> defMobSpawnerSpawnsRanges;
		// default is 200 to 600 spawns (About 30 seconds per spawn so 100 to 300 minutes til the spawner expires).
		public final String initialMobSpawnerSpawnsRanges = "harderspawners:default,200,600;" + "minecraft:pig,0,0;"
				+ "minecraft:cow,0,0;" + "minecraft:sheep,0,0;" + "minecraft:parrot,0,0;" + "minecraft:zombie,100,550;"
				+ "minecraft:blaze,0,0;";

		public Common(ModConfigSpec.Builder builder) {
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

			spawnerMinutesStunned = builder
					.comment("0- spawner breaks as normal.  Values over 0 minutes stun the spawner but don't break it.")
					.translation(Main.MODID + ".config." + "spawnerMinutesStunned")
					.defineInRange("spawnerMinutesStunned", () -> 0, 0, 27);

			spawnerBreakSpeedModifier = builder
					.comment(
							"Spawner Break Speed Modifier: 0 = Off, 1 = 50% slower, From 2 to 2.1 billion times slower")
					.translation(Main.MODID + ".config." + "spawnerBreakSpeedModifier")
					.defineInRange("spawnerBreakSpeedModifier", () -> 4, 0, Integer.MAX_VALUE);

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
					.comment("hostileSpawnerLightLevel: A custom higher light level instead of the standard light level 0.")
					.translation(Main.MODID + ".config." + "hostileSpawnerLightLevel")
					.defineInRange("hostileSpawnerLightLevel", () -> 11, 0, 15);

			hostileSpawnerResistDaylightDuration = builder
					.comment("hostileSpawnerResistDaylightDuration: give undead fire resistance (true) ")
					.translation(Main.MODID + ".config." + "hostileSpawnerResistDaylightDuration")
					.defineInRange("hostileSpawnerResistDaylightDuration", () -> 120, 0, 9999);

			builder.pop();

			builder.push("Default Mob Spawner Durability and Durability Repair Values");

			timeExtensionItem = builder.comment("Item used to extend Spawner expiration time (format: 'modid:item_name')")
					.define("timeExtensionItem", "minecraft:iron_block");

			spawnsAmount = builder.comment("How many spawns the time extension item adds.  0 = off")
					.translation(Main.MODID + ".config." + "spawnsAmount ")
					.defineInRange("spawnsAmount ", () -> 5, 0, 1000);

			defMobSpawnerSpawnsRanges = builder.comment("Default range of number of spawns a spawner can make, 0,0 = infinite")
					.translation(Main.MODID + ".config" + "defMobSpawnerSpawnsRanges")
					.define("defMobSpawnerSpawnsRanges", initialMobSpawnerSpawnsRanges);

			builder.pop();
		}

	}


	public static final int TICKS_PER_MINUTE = 1200;

	public static final Common COMMON;
	public static final ModConfigSpec COMMON_SPEC;

	static {
		final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	private static boolean configLoaded = false;
	private static int debugLevel;
	private static int spawnerTextOff;
	private static int spawnerMinutesStunned;
	private static int destroyLightPercentage;
	private static int destroyLightRange;
	private static int spawnerBreakSpeedModifier;
	private static int spawnerRevengeLevel;
	private static int maxNearbyEntities;
	private static int requiredPlayerRange;
	private static int spawnRange;
	private static int hostileSpawnerLightLevel;
	private static int hostileSpawnerResistDaylightDuration;
	private static double spawnersExplodePercentage;
	public static String timeExtensionItem;
	public static int spawnsAmount;
	private static String mobSpawnerDurabilityRangesString;

	public static void bakeConfig() {

		configLoaded = true;
		debugLevel = COMMON.debugLevel.get();
		setSpawnerTextOff(COMMON.spawnerTextOff.get());
		spawnerMinutesStunned = COMMON.spawnerMinutesStunned.get();
		setSpawnerBreakSpeedModifier(COMMON.spawnerBreakSpeedModifier.get());
		destroyLightPercentage = COMMON.destroyLightPercentage.get();
		destroyLightRange = COMMON.destroyLightRange.get();
		setSpawnersExplodePercentage(COMMON.spawnersExplodePercentage.get());
		setSpawnerRevengeLevel(COMMON.spawnerRevengeLevel.get());
		maxNearbyEntities = COMMON.maxNearbyEntities.get();
		requiredPlayerRange = COMMON.requiredPlayerRange.get();
		spawnRange = COMMON.spawnRange.get();
		hostileSpawnerLightLevel = COMMON.hostileSpawnerLightLevel.get();
		hostileSpawnerResistDaylightDuration = COMMON.hostileSpawnerResistDaylightDuration.get();
		timeExtensionItem = COMMON.timeExtensionItem.get();
		spawnsAmount = COMMON.spawnsAmount.get();
		setMobSpawnerDurabilityRangesString(COMMON.defMobSpawnerSpawnsRanges.get());

	}

	public static boolean isConfigLoaded() {
		return configLoaded;
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

	public static int getSpawnerTicksStunned() {
		return spawnerMinutesStunned * TICKS_PER_MINUTE;
	}

	public static int getSpawnRange() {
		return spawnRange;
	}

	public static String getTimeExtensionItem() {

		return timeExtensionItem;

	}



	public static boolean isTimeExtensionEnabled() {
		if (spawnsAmount > 0)
			return true;
		return false;
	}

	public static int getSpawnsAmount() {
		return spawnsAmount;
	}



	public static void pushDebugValue() {
		MyUtilities.debugMsg(1, "harderspawners debugLevel:" + MyConfig.debugLevel);
		COMMON.debugLevel.set(MyConfig.debugLevel);
	}

	public static void pushSpawnerRevenge() {
		MyUtilities.debugMsg(1, "harderspawners: revengeLevel" + MyConfig.getSpawnerRevengeLevel());
		COMMON.spawnerRevengeLevel.set(MyConfig.getSpawnerRevengeLevel());
	}

	public static void pushSpawnersExplodePercentage() {
		MyUtilities.debugMsg(1, "harderspawners: breaking explode % :" + MyConfig.getSpawnersExplodePercentage());
		COMMON.spawnersExplodePercentage.set(MyConfig.getSpawnersExplodePercentage());
	}

	public static void setDebugLevel(int debugLevel) {
		MyConfig.debugLevel = debugLevel;
	}

	public static int getSpawnerBreakSpeedModifier() {
		return spawnerBreakSpeedModifier;
	}

	public static void setSpawnerBreakSpeedModifier(int spawnerBreakSpeedMultiplier) {
		MyConfig.spawnerBreakSpeedModifier = spawnerBreakSpeedMultiplier;
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

	public static String getMobSpawnerDurabilityRangesString() {
		return mobSpawnerDurabilityRangesString;
	}

	public static void setMobSpawnerDurabilityRangesString(String stringIn) {
		MyConfig.mobSpawnerDurabilityRangesString = stringIn;
	}

	public static double getSpawnersExplodePercentage() {
		return spawnersExplodePercentage;
	}

	public static void setSpawnersExplodePercentage(double spawnersExplodePercentage) {
		MyConfig.spawnersExplodePercentage = spawnersExplodePercentage;
	}

}
