package com.mactso.harderspawners.modloader.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.modloader.main.Main;
import com.mojang.datafixers.util.Pair;

/**
 * Fabric-style runtime configuration for Harder Spawners.
 * 
 * Follows the same pattern as Regrowth: uses SimpleConfig provider, maintains
 * defaults, and allows runtime push/pull of config values.
 */
public class MyConfig {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final int TICKS_PER_MINUTE = 20 * 60;

	public static SimpleConfig CONFIG;
	private static ModConfigProvider configs;

	// Default strings for spawner lifespan, mob ranges, etc.
	private static final String defaultMobSpawnerRanges = "harderspawners:default,200,600;" + "minecraft:pig,0,0;"
			+ "minecraft:cow,0,0;" + "minecraft:sheep,0,0;" + "minecraft:parrot,0,0;" + "minecraft:zombie,100,550;"
			+ "minecraft:blaze,0,0;";

	/*
	 * ----------------------------- Runtime Config Values
	 * -----------------------------
	 */
	public static int debugLevel;
	public static int spawnerTextOff;

	public static int spawnerMinutesStunned;
	public static int spawnerBreakSpeedModifier;
	public static int spawnerRevengeLevel;
	public static int spawnersExplodePercentage;

	public static int destroyLightPercentage;
	public static int destroyLightRange;
	public static int hostileSpawnerLightLevel;
	public static int hostileSpawnerResistDaylightDuration;

	public static int requiredPlayerRange;
	public static int maxNearbyEntities;
	public static int spawnRange;

	public static boolean preserveNonVanillaSpawnerTiming;
	public static int minSpawnDelayOverride;
	public static int maxSpawnDelayOverride;

	public static String addLifespanItem;
	public static int extraLifespanAmount;
	public static String mobSpawnerDurabilityRangesString;
	public static String endOfLifeActionString;
	public static EndOfLifespanAction endOfLifespanAction;

	public enum EndOfLifespanAction {
		DESTROYED, LINGER
	}

	/*
	 * ----------------------------- Register & Load -----------------------------
	 */
	public static void registerConfigs() {
		configs = new ModConfigProvider();
		createConfigs();
		CONFIG = SimpleConfig.of(Main.MODID + "_config").provider(configs).request();
		assignConfigs();
	}

	private static void createConfigs() {

		// ===== Debug =====
		configs.addComment("Debug settings");
		configs.addKeyValuePair(new Pair<>("debugLevel", 0), "int", "0-2; 0 off, 1 some detail, 2 high detail.");
		configs.addKeyValuePair(new Pair<>("spawnerTextOff", 1), "int", "0=show text, 1=hide text");

		// ===== Spawner behavior =====
		configs.addComment("");
		configs.addComment("Spawner behavior settings");
		configs.addKeyValuePair(new Pair<>("spawnerMinutesStunned", 0), "int", "0=Spawner Breaks, 1 to 27 = minutes Spawner stunned");
		configs.addKeyValuePair(new Pair<>("spawnerBreakSpeedModifier", 1), "int",
				"Slows mining of Spawners.  -1=Feature off. 0-3 = mining fatigue 1-4");
		configs.addKeyValuePair(new Pair<>("spawnerRevengeLevel", 1), "int", "Range 0-11.  0=Off.  1 to 11 increases relation power");
		configs.addKeyValuePair(new Pair<>("spawnersExplodePercentage", 33), "int",
				"0 = 0ff.  0-100% chance of explosions.");

		// ===== Environment =====
		configs.addComment("");
		configs.addComment("Environmental settings");
		configs.addKeyValuePair(new Pair<>("destroyLightPercentage", 100), "int",
				"0-100% chance to destroy light emitting blocks.");
		configs.addKeyValuePair(new Pair<>("destroyLightRange", 7), "int", "1 to 9 block range from spawner");
		configs.addKeyValuePair(new Pair<>("hostileSpawnerLightLevel", 11), "int",
				"0-15; allows spawning in higher light levels.");
		configs.addKeyValuePair(new Pair<>("hostileSpawnerResistDaylightDuration", 120), "int",
				"0 to 2 billion Seconds of fire resist for undead mobs spawned by this spawner.");

		// ===== Spawning mechanics =====
		configs.addComment("");
		configs.addComment("Spawning mechanics");
		configs.addKeyValuePair(new Pair<>("requiredPlayerRange", 13), "int",
				"When players are within this range (6 to 24) the spawner starts spawning.");
		configs.addKeyValuePair(new Pair<>("maxNearbyEntities", 9), "int",
				"6 to 15 maximum number of mobs nearby before spawn blocked");
		configs.addKeyValuePair(new Pair<>("spawnRange", 9), "int", "6 to 32 Mobs spawn within 0 to value blocks from spawner.");

		// ===== Spawn delay overrides =====
		configs.addComment("");
		configs.addComment("Spawn delay overrides");
		configs.addKeyValuePair(new Pair<>("preserveNonVanillaSpawnerTiming", true), "boolean",
				"true/false; keep vanilla spawner timing");
		configs.addKeyValuePair(new Pair<>("minSpawnDelayOverride", 200), "int", "60 to 600 ticks; min spawn delay");
		configs.addKeyValuePair(new Pair<>("maxSpawnDelayOverride", 800), "int", "600 to 6000 ticks; max spawn delay");
		configs.addComment("Note max must be > min or it resets to max = min + 100");

		// ===== Lifespan  =====
		configs.addComment("");
		configs.addComment("Lifespan");
		configs.addKeyValuePair(new Pair<>("addLifespanItem", "minecraft:iron_block"), "String",
				"item id; block or item consumed to increase spawner lifespan. ");
		configs.addKeyValuePair(new Pair<>("extraLifespanAmount", 5), "int", "0 to 320,000 ticks added to spawner lifespan");
		configs.addComment("List of default, and mobs default (min to max) number of spawns.");
		configs.addKeyValuePair(new Pair<>("mobSpawnerDurabilityRangesString", defaultMobSpawnerRanges), "String",
				"format: modid:name,min,max; ...");
		configs.addKeyValuePair(new Pair<>("endOfLifespanAction", "DESTROYED"), "String",
				"DESTROYED/LINGER; action at end of lifespan");
		configs.addComment("DESTROYED: Spawner breaks or explodes.");
		configs.addComment("LINGER: does not break but only spawns every 25 minutes until repaired.");
		
	}

	private static void assignConfigs() {

		debugLevel = CONFIG.getOrDefault("debugLevel", 0);
		debugLevel = clampVariable(debugLevel, 0, 100);

		spawnerTextOff = CONFIG.getOrDefault("spawnerTextOff", 1);

		spawnerMinutesStunned = CONFIG.getOrDefault("spawnerMinutesStunned", 0);
		spawnerMinutesStunned = clampVariable(spawnerMinutesStunned, 0, 27);
		// -1,0,1,2,3
		spawnerBreakSpeedModifier = CONFIG.getOrDefault("spawnerBreakSpeedModifier", 1);
		spawnerBreakSpeedModifier = clampVariable(spawnerBreakSpeedModifier, -1, 3);

		spawnerRevengeLevel = CONFIG.getOrDefault("spawnerRevengeLevel", 1);
		spawnerRevengeLevel = clampVariable(spawnerRevengeLevel, 0, 11);

		spawnersExplodePercentage = CONFIG.getOrDefault("spawnersExplodePercentage", 33);
		spawnersExplodePercentage = clampVariable(spawnerRevengeLevel, 0, 11);

		destroyLightPercentage = CONFIG.getOrDefault("destroyLightPercentage", 100);
		destroyLightPercentage = clampVariable(destroyLightPercentage, 0, 100);

		destroyLightRange = CONFIG.getOrDefault("destroyLightRange", 7);
		destroyLightRange = clampVariable(destroyLightRange, 1, 9);

		hostileSpawnerLightLevel = CONFIG.getOrDefault("hostileSpawnerLightLevel", 11);
		hostileSpawnerLightLevel = clampVariable(hostileSpawnerLightLevel, 0, 15);

		hostileSpawnerResistDaylightDuration = CONFIG.getOrDefault("hostileSpawnerResistDaylightDuration", 120);
		hostileSpawnerResistDaylightDuration = clampVariable(hostileSpawnerResistDaylightDuration, 0,
				Integer.MAX_VALUE);

		requiredPlayerRange = CONFIG.getOrDefault("requiredPlayerRange", 13);
		requiredPlayerRange = clampVariable(requiredPlayerRange, 6, 24);

		maxNearbyEntities = CONFIG.getOrDefault("maxNearbyEntities", 9);
		maxNearbyEntities = clampVariable(maxNearbyEntities, 6, 15);

		spawnRange = CONFIG.getOrDefault("spawnRange", 9);
		spawnRange = clampVariable(spawnRange, 6, 32);

		preserveNonVanillaSpawnerTiming = CONFIG.getOrDefault("preserveNonVanillaSpawnerTiming", true);

		minSpawnDelayOverride = CONFIG.getOrDefault("minSpawnDelayOverride", 200);
		minSpawnDelayOverride = clampVariable(minSpawnDelayOverride, 60, 500);

		maxSpawnDelayOverride = CONFIG.getOrDefault("maxSpawnDelayOverride", 800);
		maxSpawnDelayOverride = clampVariable(maxSpawnDelayOverride, 600, 6000);

		if (maxSpawnDelayOverride < minSpawnDelayOverride)
			maxSpawnDelayOverride = minSpawnDelayOverride + 100;

		addLifespanItem = CONFIG.getOrDefault("addLifespanItem", "minecraft:iron_block");
		extraLifespanAmount = CONFIG.getOrDefault("extraLifespanAmount", 5);
		extraLifespanAmount = clampVariable(extraLifespanAmount, 0, 320000);

		mobSpawnerDurabilityRangesString = CONFIG.getOrDefault("mobSpawnerDurabilityRangesString",
				defaultMobSpawnerRanges);
		endOfLifeActionString = CONFIG.getOrDefault("endOfLifespanAction", "DESTROYED").toUpperCase();
		if (endOfLifeActionString.equalsIgnoreCase("LINGER")) {
			endOfLifeActionString = "LINGER"; // fallback for invalid user input
		} else {
			endOfLifeActionString = "DESTROY";
		}

		LOGGER.info("HarderSpawners Fabric config loaded successfully.");
		reportConfig();
	}

	public static void reportConfig() {
		LOGGER.info("");
		LOGGER.info("=== HarderSpawners Configuration Report ===");

		// Numeric int configs with min/max, showing current loaded value
		LOGGER.info("debugLevel, min=0, max=unbounded, default=0, value=" + getDebugLevel());
		LOGGER.info("spawnerTextOff, min=0, max=unbounded, default=1, value=" + getSpawnerTextOff());
		LOGGER.info("spawnerMinutesStunned, min=0, max=unbounded, default=0, value=" + getSpawnerMinutesStunned());
		LOGGER.info("spawnerBreakSpeedModifier, min=-1, max=4, default=1, value=" + getSpawnerBreakSpeedModifier());
		LOGGER.info("spawnerRevengeLevel, min=0, max=11, default=1, value=" + getSpawnerRevengeLevel());
		LOGGER.info("spawnersExplodePercentage, min=0, max=100, default=33, value=" + getSpawnersExplodePercentage());
		LOGGER.info("destroyLightPercentage, min=0, max=100, default=100, value=" + getDestroyLightPercentage());
		LOGGER.info("destroyLightRange, min=6, max=9, default=7, value=" + getDestroyLightRange());
		LOGGER.info("hostileSpawnerLightLevel, min=0, max=15, default=11, value=" + getHostileSpawnerLightLevel());
		LOGGER.info("hostileSpawnerResistDaylightDuration, min=0, max=unbounded, default=120, value="
				+ getHostileSpawnerResistDaylightDuration());
		LOGGER.info("requiredPlayerRange, min=6, max=15, default=13, value=" + getRequiredPlayerRange());
		LOGGER.info("maxNearbyEntities, min=6, max=15, default=9, value=" + getMaxNearbyEntities());
		LOGGER.info("spawnRange, min=6, max=32, default=9, value=" + getSpawnRange());
		LOGGER.info("minSpawnDelayOverride, min=60, max=500, default=200, value=" + getMinSpawnDelayOverride());
		LOGGER.info("maxSpawnDelayOverride, min=600, max=6000, default=800, value=" + getMaxSpawnDelayOverride());
		LOGGER.info("extraLifespanAmount, min=0, max=320000, default=5, value=" + getExtraLifespanAmount());

		// Boolean configs
		LOGGER.info("preserveNonVanillaSpawnerTiming, true|false, default=true, value="
				+ isPreserveNonVanillaSpawnerTiming());

		// String configs
		LOGGER.info("addLifespanItem, default=minecraft:iron_block, value=" + getAddLifespanItem());
		LOGGER.info("mobSpawnerDurabilityRangesString, default=" + defaultMobSpawnerRanges.replace(";", "; ")
				+ ", value=" + getMobSpawnerLifespanRangesString().replace(";", "; "));
		LOGGER.info("endOfLifespanAction, default=DESTROYED, value=" + getEndOfLifespanAction());

		LOGGER.info("=== End of Configuration Report ===\n");
	}

	/*
	 * ----------------------------- Runtime Getters -----------------------------
	 */

	// -------------------------------------------------------------------------
// Getters (Fabric-compatible)
// -------------------------------------------------------------------------

	public static int clampVariable(int variable, int min, int max) {
		return Math.max(min, Math.min(max, variable));
	}

	public static float clampVariable(float variable, float min, float max) {
		return Math.max(min, Math.min(max, variable));
	}

	public static boolean isConfigLoaded() {
		return CONFIG != null;
	}

	public static int getDebugLevel() {
		return CONFIG.getOrDefault("debugLevel", 0);
	}

	public static boolean isDebug() {
		return getDebugLevel() > 0;
	}

	public static int getSpawnerBreakSpeedModifier() {
		return spawnerBreakSpeedModifier;
	}

	public static int getDestroyLightPercentage() {
		return CONFIG.getOrDefault("destroyLightPercentage", 100);
	}

	public static int getDestroyLightRange() {
		return CONFIG.getOrDefault("destroyLightRange", 7);
	}

	public static int getHostileSpawnerLightLevel() {
		return CONFIG.getOrDefault("hostileSpawnerLightLevel", 11);
	}

	public static int getHostileSpawnerResistDaylightDuration() {
		return CONFIG.getOrDefault("hostileSpawnerResistDaylightDuration", 120);
	}

	public static int getMaxNearbyEntities() {
		return CONFIG.getOrDefault("maxNearbyEntities", 9);
	}

	public static int getRequiredPlayerRange() {
		return CONFIG.getOrDefault("requiredPlayerRange", 13);
	}

	public static int getSpawnRange() {
		return CONFIG.getOrDefault("spawnRange", 9);
	}

	public static int getSpawnerMinutesStunned() {
		return CONFIG.getOrDefault("spawnerMinutesStunned", 0);
	}

	public static int getSpawnerTicksStunned() {
		return getSpawnerMinutesStunned() * TICKS_PER_MINUTE;
	}

	public static boolean isPreserveNonVanillaSpawnerTiming() {
		return CONFIG.getOrDefault("preserveNonVanillaSpawnerTiming", true);
	}

	public static int getMinSpawnDelayOverride() {
		return CONFIG.getOrDefault("minSpawnDelayOverride", 200);
	}

	public static int getMaxSpawnDelayOverride() {
		return CONFIG.getOrDefault("maxSpawnDelayOverride", 800);
	}

	public static int getSpawnerRevengeLevel() {
		return CONFIG.getOrDefault("spawnerRevengeLevel", 1);
	}

// -------------------- Lifespan & EndOfLifespanAction ----------------------

	public static int getSpawnsAmount() {
		return CONFIG.getOrDefault("extraLifespanAmount", 5);
	}

	public static boolean isAddLifespanEnabled() {
		return getSpawnsAmount() > 0;
	}

	public static String getAddLifespanItem() {
		return CONFIG.getOrDefault("addLifespanItem", "minecraft:iron_block");
	}

	public static int getExtraLifespanAmount() {
		return getSpawnsAmount();
	}

	public static EndOfLifespanAction getEndOfLifespanAction() {
		if (endOfLifeActionString.equalsIgnoreCase("LINGER")) {
			return EndOfLifespanAction.LINGER;
		} else {
			return EndOfLifespanAction.DESTROYED;

		}
	}

	public static String getMobSpawnerLifespanRangesString() {
		return CONFIG.getOrDefault("defMobSpawnerSpawnsRanges",
				"harderspawners:default,200,600;minecraft:pig,0,0;minecraft:cow,0,0;"
						+ "minecraft:sheep,0,0;minecraft:parrot,0,0;minecraft:zombie,100,550;minecraft:blaze,0,0;");
	}

	public static int getSpawnerTextOff() {
		return CONFIG.getOrDefault("spawnerTextOff", 1);
	}

	public static double getSpawnersExplodePercentage() {
		return CONFIG.getOrDefault("spawnersExplodePercentage", 33.1); // 33.1 helps see failure case.
	}

}
