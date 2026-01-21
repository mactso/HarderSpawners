
package com.mactso.harderspawners.modloader.config;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.main.Main;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Central configuration definition and runtime access point for Harder
 * Spawners.
 *
 * <p>
 * This class defines all NeoForge {@link ModConfigSpec} values used by the mod,
 * grouped into logical sections such as debug options, spawner behavior,
 * spawning mechanics, environmental effects, and spawner lifespan.
 * </p>
 *
 * <p>
 * Configuration values are baked into static runtime fields on load or reload
 * via {@link #bakeConfig()} to allow fast access during gameplay without
 * repeatedly querying the config system.
 * </p>
 */
@EventBusSubscriber(modid = Main.MODID)
public class MyConfig {

	public enum EndOfLifespanAction {
	    DESTROYED,
	    LINGER
	}
	/*
	 * -------------------------------------------------------------------------
	 * Config load handling
	 * ----------------------------------------------------------------------
	 */
	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent) {
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();

		}
	}

	/*
	 * -------------------------------------------------------------------------
	 * Config specification
	 * ----------------------------------------------------------------------
	 */
	public static class Common {
		/*
		 * ----------------------------- Debug & Messaging --------------------------
		 */
		public final IntValue debugLevel;
		public final IntValue spawnerTextOff;

		/*
		 * ----------------------------- Spawner interaction & punishment
		 * --------------------------
		 */

		public final IntValue spawnerMinutesStunned;
		public final IntValue spawnerBreakSpeedModifier;
		public final IntValue spawnerRevengeLevel;
		public final DoubleValue spawnersExplodePercentage;

		/*
		 * ----------------------------- Environmental & light effects
		 * --------------------------
		 */

		public final IntValue destroyLightPercentage;
		public final IntValue destroyLightRange;
		public final IntValue hostileSpawnerLightLevel;
		public final IntValue hostileSpawnerResistDaylightDuration;

		/*
		 * ----------------------------- Spawning mechanics --------------------------
		 */

		public final IntValue requiredPlayerRange;
		public final IntValue maxNearbyEntities;
		public final IntValue spawnRange;

		/*
		 * ----------------------------- Spawn delay overrides
		 * --------------------------
		 */

		public final BooleanValue preserveNonVanillaSpawnerTiming;
		public final IntValue minSpawnDelayOverride;
		public final IntValue maxSpawnDelayOverride;

		/*
		 * ----------------------------- Spawner lifespan values
		 * --------------------------
		 */

		public final ConfigValue<String> addLifespanItem;
		public final IntValue extraLifespanAmount;
		public final ConfigValue<String> defMobSpawnerSpawnsRanges;
		public final EnumValue<EndOfLifespanAction> endOfLifespanAction;
		/**
		 * Default number of spawn ranges applied to spawners when no override exists.
		 * Format: modid:entity,minspawns,maxspawns; 0,0 means infinite. The actual
		 * number of spawns are used to calculate the expiration time.
		 */
		

		public final String initialMobSpawnerSpawnsRanges = "harderspawners:default,200,600;" + "minecraft:pig,0,0;"
				+ "minecraft:cow,0,0;" + "minecraft:sheep,0,0;" + "minecraft:parrot,0,0;" + "minecraft:zombie,100,550;"
				+ "minecraft:blaze,0,0;";

		public Common(ModConfigSpec.Builder builder) {
			builder.push("Harder Spawners Control Values");
			/*
			 * ============================= Debug & Messaging ===========================
			 */

			builder.push("debug");

			debugLevel = builder.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat + Log")
					.translation(Main.MODID + ".config.debugLevel").defineInRange("debugLevel", 0, 0, 2);

			spawnerTextOff = builder.comment("0 = spawner chat messages on, 1 = spawner chat messages off")
					.translation(Main.MODID + ".config.spawnerTextOff").defineInRange("spawnerTextOff", 1, 0, 1);

			builder.pop();

			/*
			 * ============================= Spawner interaction & punishment
			 * ===========================
			 */

			builder.push("spawner_behavior");

			spawnerMinutesStunned = builder
					.comment("Minutes a spawner is stunned instead of breaking. 0 = normal break.")
					.translation(Main.MODID + ".config.spawnerMinutesStunned")
					.defineInRange("spawnerMinutesStunned", 0, 0, 27);

			spawnerBreakSpeedModifier = builder
					.comment("Spawner break speed modifier. 0 = off, higher values = slower break speed.")
					.translation(Main.MODID + ".config.spawnerBreakSpeedModifier")
					.defineInRange("spawnerBreakSpeedModifier", 4, 0, Integer.MAX_VALUE);

			spawnerRevengeLevel = builder.comment("Spawner revenge level. 0 = off, higher values increase retaliation.")
					.translation(Main.MODID + ".config.spawnerRevengeLevel")
					.defineInRange("spawnerRevengeLevel", 1, 0, 11);

			spawnersExplodePercentage = builder.comment("Chance (percent) that a spawner explodes when broken.")
					.translation(Main.MODID + ".config.spawnersExplodePercentage")
					.defineInRange("spawnersExplodePercentage", 33.0, 0.0, 100.0);

			builder.pop();

			/*
			 * ============================= Environmental & light effects
			 * ===========================
			 */

			builder.push("environment");

			destroyLightPercentage = builder.comment("Chance (percent) to destroy nearby light sources.")
					.translation(Main.MODID + ".config.destroyLightPercentage")
					.defineInRange("destroyLightPercentage", 100, 0, 100);

			destroyLightRange = builder.comment("Range in blocks for light destruction.")
					.translation(Main.MODID + ".config.destroyLightRange").defineInRange("destroyLightRange", 7, 1, 7);

			hostileSpawnerLightLevel = builder
					.comment("Custom maximum light level at which hostile spawners can spawn mobs.")
					.translation(Main.MODID + ".config.hostileSpawnerLightLevel")
					.defineInRange("hostileSpawnerLightLevel", 11, 0, 15);

			hostileSpawnerResistDaylightDuration = builder
					.comment("Duration in ticks that undead spawned from spawners resist daylight fire.")
					.translation(Main.MODID + ".config.hostileSpawnerResistDaylightDuration")
					.defineInRange("hostileSpawnerResistDaylightDuration", 120, 0, 9999);

			builder.pop();

			/*
			 * ============================= Spawning mechanics ===========================
			 */

			builder.push("spawning");

			requiredPlayerRange = builder.comment("Player distance required for hostile spawners to activate.")
					.translation(Main.MODID + ".config.requiredPlayerRange")
					.defineInRange("requiredPlayerRange", 13, 7, 256);

			maxNearbyEntities = builder.comment("Maximum number of nearby hostile entities allowed.")
					.translation(Main.MODID + ".config.maxNearbyEntities")
					.defineInRange("maxNearbyEntities", 9, 3, 256);

			spawnRange = builder.comment("Maximum distance from spawner where mobs may spawn.")
					.translation(Main.MODID + ".config.spawnRange").defineInRange("spawnRange", 9, 4, 256);

			builder.pop();

			/*
			 * ============================= Spawn delay overrides
			 * ===========================
			 */

			builder.push("spawn_delay");

			preserveNonVanillaSpawnerTiming = builder
					.comment("If true, spawn delays are not overridden when spawners already",
							"have non-vanilla timing values. Vanilla is Min=200, Max=800.")
					.define("preserveNonVanillaSpawnerTiming", true);

			minSpawnDelayOverride = builder.comment("Minimum spawn delay override in ticks (vanilla is 200).")
					.defineInRange("minSpawnDelayOverride", 200, 60, 540);

			maxSpawnDelayOverride = builder.comment("Maximum spawn delay override in ticks (vanilla is 800).")
					.defineInRange("maxSpawnDelayOverride", 800, 600, 10_000);

			builder.pop();

			/*
			 * ============================= Spawner durability & repair
			 * ===========================
			 */

			builder.push("spawner_lifespan");

			addLifespanItem = builder.comment("This item adds time to the spawner lifespan.  (format: modid:item).")
					.define("addLifespanItem", "minecraft:iron_block");

			extraLifespanAmount = builder.comment("Number of additional spawns granted by the addLifespanItem. 0 = disabled.")
					.translation(Main.MODID + ".config.spawnsAmount").defineInRange("extraLifespanAmount", 5, 0, 10000);

			defMobSpawnerSpawnsRanges = builder.comment("Default spawn count ranges per entity. 0,0 = infinite.")
					.translation(Main.MODID + ".config.defMobSpawnerSpawnsRanges")
					.define("defMobSpawnerSpawnsRanges", initialMobSpawnerSpawnsRanges);
			endOfLifespanAction = builder
				    .comment("Determines what happens when a spawner reaches the end of its lifespan.",
				             "DESTROYED - Spawner will be destroyed and may explode.",
				             "LINGER - Spawner will only spawn ever 25 minutes.  Their lifespan can still be increased.")
				    .translation(Main.MODID + ".config.endOfLifespanAction")
				    .defineEnum("endOfLifespanAction", EndOfLifespanAction.DESTROYED);
			builder.pop();
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

	/*
	 * -------------------------------------------------------------------------
	 * Runtime values (baked)
	 * ----------------------------------------------------------------------
	 */

	private static boolean configLoaded;

	// Debug & messaging
	private static int debugLevel;
	private static int spawnerTextOff;

	// Spawner behavior
	private static int spawnerMinutesStunned;
	private static int spawnerBreakSpeedModifier;
	private static int spawnerRevengeLevel;
	private static double spawnersExplodePercentage;

	// Environmental
	private static int destroyLightPercentage;
	private static int destroyLightRange;
	private static int hostileSpawnerLightLevel;
	private static int hostileSpawnerResistDaylightDuration;

	// Spawning mechanics
	private static int requiredPlayerRange;
	private static int maxNearbyEntities;
	private static int spawnRange;

	// Spawn delay
	private static boolean preserveNonVanillaSpawnerTiming;
	private static int minSpawnDelayOverride;
	private static int maxSpawnDelayOverride;

	// Number of Spawns and Expiration Time values. 
	public static String addLifespanItem;
	public static int extraLifespanAmount;
	private static String mobSpawnerDurabilityRangesString;
	private static Enum<EndOfLifespanAction> endOfLifespanAction;


	/* -------------------------------------------------------------------------
	 *  Baking
	 * ---------------------------------------------------------------------- */
	
	public static void bakeConfig() {

		configLoaded = true;

		debugLevel = COMMON.debugLevel.get();
		spawnerTextOff = COMMON.spawnerTextOff.get();

		spawnerMinutesStunned = COMMON.spawnerMinutesStunned.get();
		spawnerBreakSpeedModifier = COMMON.spawnerBreakSpeedModifier.get();
		spawnerRevengeLevel = COMMON.spawnerRevengeLevel.get();
		spawnersExplodePercentage = COMMON.spawnersExplodePercentage.get();

		destroyLightPercentage = COMMON.destroyLightPercentage.get();
		destroyLightRange = COMMON.destroyLightRange.get();
		hostileSpawnerLightLevel = COMMON.hostileSpawnerLightLevel.get();
		hostileSpawnerResistDaylightDuration = COMMON.hostileSpawnerResistDaylightDuration.get();

		requiredPlayerRange = COMMON.requiredPlayerRange.get();
		maxNearbyEntities = COMMON.maxNearbyEntities.get();
		spawnRange = COMMON.spawnRange.get();

		preserveNonVanillaSpawnerTiming = COMMON.preserveNonVanillaSpawnerTiming.get();
		minSpawnDelayOverride = COMMON.minSpawnDelayOverride.get();
		maxSpawnDelayOverride = COMMON.maxSpawnDelayOverride.get();

		addLifespanItem = COMMON.addLifespanItem.get();
		extraLifespanAmount = COMMON.extraLifespanAmount.get();
		mobSpawnerDurabilityRangesString = COMMON.defMobSpawnerSpawnsRanges.get();
		endOfLifespanAction = COMMON.endOfLifespanAction.get();

	}


	/*
	 * -------------------------------------------------------------------------
	 * Getters (unchanged semantics)
	 * ----------------------------------------------------------------------
	 */




	public static boolean isConfigLoaded() {
		return configLoaded;
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static boolean isDebug() {
		return debugLevel > 0;
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

	public static int getSpawnRange() {
		return spawnRange;
	}

	public static int getSpawnerMinutesStunned() {
		return spawnerMinutesStunned;
	}

	public static int getSpawnerTicksStunned() {
		return spawnerMinutesStunned * TICKS_PER_MINUTE;
	}

	public static boolean isPreserveNonVanillaSpawnerTiming() {
		return preserveNonVanillaSpawnerTiming;
	}

	public static int getMinSpawnDelayOverride() {
		return minSpawnDelayOverride;
	}

	public static int getMaxSpawnDelayOverride() {
		return maxSpawnDelayOverride;
	}
	public static int getSpawnerRevengeLevel() {
		return spawnerRevengeLevel;
	}
	

	public static int getSpawnsAmount() {
		return extraLifespanAmount;
	}

	public static boolean isAddLifespanEnabled() {
		return extraLifespanAmount > 0;
	}

	public static String getAddLifespanItem() {
		return addLifespanItem;
	}
	
	public static int getExtraLifespanAmount() {
		return extraLifespanAmount;
	}

	public static Enum<EndOfLifespanAction> getEndOfLifespanAction() {
		return endOfLifespanAction;
	}


	public static String getMobSpawnerLifespanRangesString() {
		return mobSpawnerDurabilityRangesString;
	}

	public static int getSpawnerTextOff() {
		return spawnerTextOff;
	}

	public static double getSpawnersExplodePercentage() {
		return spawnersExplodePercentage;
	}

	
	/*
	 * -------------------------------------------------------------------------
	 * Push / setters (unchanged)
	 * ----------------------------------------------------------------------
	 */

	public static void pushDebugValue() {
		MyUtilities.debugMsg(1, "harderspawners debugLevel:" + debugLevel);
		COMMON.debugLevel.set(debugLevel);
	}

	public static void pushSpawnerRevenge() {
		MyUtilities.debugMsg(1, "harderspawners revengeLevel:" + spawnerRevengeLevel);
		COMMON.spawnerRevengeLevel.set(spawnerRevengeLevel);
	}

	public static void pushSpawnersExplodePercentage() {
		MyUtilities.debugMsg(1, "harderspawners explode %:" + spawnersExplodePercentage);
		COMMON.spawnersExplodePercentage.set(spawnersExplodePercentage);
	}

	public static int getSpawnerBreakSpeedModifier() {
		return spawnerBreakSpeedModifier;
	}

	public static void setSpawnerBreakSpeedModifier(int v) {
		spawnerBreakSpeedModifier = v;
	}


	public static void setMobSpawnerDurabilityRangesString(String stringIn) {
		MyConfig.mobSpawnerDurabilityRangesString = stringIn;
	}

	public static void setSpawnerRevengeLevel(int v) {
		spawnerRevengeLevel = v;
	}
	
	public static void setSpawnerTextOff(int v) {
		spawnerTextOff = v;
	}

	public static void setSpawnersExplodePercentage(double v) {
		spawnersExplodePercentage = v;
	}
	
}
