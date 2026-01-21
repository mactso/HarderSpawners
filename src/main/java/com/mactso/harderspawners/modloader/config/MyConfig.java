package com.mactso.harderspawners.modloader.config;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.modloader.main.Main;
import com.mojang.datafixers.util.Pair;

/**
 * Fabric-style runtime configuration for Harder Spawners.
 * 
 * Follows the same pattern as Regrowth: uses SimpleConfig provider,
 * maintains defaults, and allows runtime push/pull of config values.
 */
public class MyConfig {

    private static final Logger LOGGER = LogManager.getLogger();

    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;

    // Default strings for spawner lifespan, mob ranges, etc.
    private static final String defaultMobSpawnerRanges =
        "harderspawners:default,200,600;" +
        "minecraft:pig,0,0;" +
        "minecraft:cow,0,0;" +
        "minecraft:sheep,0,0;" +
        "minecraft:parrot,0,0;" +
        "minecraft:zombie,100,550;" +
        "minecraft:blaze,0,0;";

    /*
     * -----------------------------
     * Runtime Config Values
     * -----------------------------
     */
    public static int debugLevel;
    public static int spawnerTextOff;

    public static int spawnerMinutesStunned;
    public static int spawnerBreakSpeedModifier;
    public static int spawnerRevengeLevel;
    public static double spawnersExplodePercentage;

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
    public static EndOfLifespanAction endOfLifespanAction;

    public enum EndOfLifespanAction {
        DESTROYED,
        LINGER
    }

    /*
     * -----------------------------
     * Register & Load
     * -----------------------------
     */
    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();
        CONFIG = SimpleConfig.of(Main.MODID + "_config").provider(configs).request();
        assignConfigs();
    }

    private static void createConfigs() {
        // Debug
        configs.addKeyValuePair(new Pair<>("debugLevel", 0), "int");
        configs.addKeyValuePair(new Pair<>("spawnerTextOff", 1), "int");

        // Spawner behavior
        configs.addKeyValuePair(new Pair<>("spawnerMinutesStunned", 0), "int");
        configs.addKeyValuePair(new Pair<>("spawnerBreakSpeedModifier", 4), "int");
        configs.addKeyValuePair(new Pair<>("spawnerRevengeLevel", 1), "int");
        configs.addKeyValuePair(new Pair<>("spawnersExplodePercentage", 33.0), "double");

        // Environment
        configs.addKeyValuePair(new Pair<>("destroyLightPercentage", 100), "int");
        configs.addKeyValuePair(new Pair<>("destroyLightRange", 7), "int");
        configs.addKeyValuePair(new Pair<>("hostileSpawnerLightLevel", 11), "int");
        configs.addKeyValuePair(new Pair<>("hostileSpawnerResistDaylightDuration", 120), "int");

        // Spawning mechanics
        configs.addKeyValuePair(new Pair<>("requiredPlayerRange", 13), "int");
        configs.addKeyValuePair(new Pair<>("maxNearbyEntities", 9), "int");
        configs.addKeyValuePair(new Pair<>("spawnRange", 9), "int");

        // Spawn delay overrides
        configs.addKeyValuePair(new Pair<>("preserveNonVanillaSpawnerTiming", true), "boolean");
        configs.addKeyValuePair(new Pair<>("minSpawnDelayOverride", 200), "int");
        configs.addKeyValuePair(new Pair<>("maxSpawnDelayOverride", 800), "int");

        // Lifespan & durability
        configs.addKeyValuePair(new Pair<>("addLifespanItem", "minecraft:iron_block"), "String");
        configs.addKeyValuePair(new Pair<>("extraLifespanAmount", 5), "int");
        configs.addKeyValuePair(new Pair<>("mobSpawnerDurabilityRangesString", defaultMobSpawnerRanges), "String");
		configs.addKeyValuePair(new Pair<>("endOfLifespanAction", "DESTROYED"), "String");
    }

    private static void assignConfigs() {
        debugLevel = CONFIG.getOrDefault("debugLevel", 0);
        spawnerTextOff = CONFIG.getOrDefault("spawnerTextOff", 1);

        spawnerMinutesStunned = CONFIG.getOrDefault("spawnerMinutesStunned", 0);
        spawnerBreakSpeedModifier = CONFIG.getOrDefault("spawnerBreakSpeedModifier", 4);
        spawnerRevengeLevel = CONFIG.getOrDefault("spawnerRevengeLevel", 1);
        spawnersExplodePercentage = CONFIG.getOrDefault("spawnersExplodePercentage", 33.0);

        destroyLightPercentage = CONFIG.getOrDefault("destroyLightPercentage", 100);
        destroyLightRange = CONFIG.getOrDefault("destroyLightRange", 7);
        hostileSpawnerLightLevel = CONFIG.getOrDefault("hostileSpawnerLightLevel", 11);
        hostileSpawnerResistDaylightDuration = CONFIG.getOrDefault("hostileSpawnerResistDaylightDuration", 120);

        requiredPlayerRange = CONFIG.getOrDefault("requiredPlayerRange", 13);
        maxNearbyEntities = CONFIG.getOrDefault("maxNearbyEntities", 9);
        spawnRange = CONFIG.getOrDefault("spawnRange", 9);

        preserveNonVanillaSpawnerTiming = CONFIG.getOrDefault("preserveNonVanillaSpawnerTiming", true);
        minSpawnDelayOverride = CONFIG.getOrDefault("minSpawnDelayOverride", 200);
        maxSpawnDelayOverride = CONFIG.getOrDefault("maxSpawnDelayOverride", 800);

        addLifespanItem = CONFIG.getOrDefault("addLifespanItem", "minecraft:iron_block");
        extraLifespanAmount = CONFIG.getOrDefault("extraLifespanAmount", 5);
        mobSpawnerDurabilityRangesString = CONFIG.getOrDefault("mobSpawnerDurabilityRangesString", defaultMobSpawnerRanges);
String actionString = CONFIG.getOrDefault("endOfLifespanAction", "DESTROYED").toUpperCase();
if (!actionString.equals("DESTROYED") && !actionString.equals("LINGER")) {
    actionString = "DESTROYED"; // fallback for invalid user input
}
endOfLifespanAction = actionString;
        LOGGER.info("HarderSpawners Fabric config loaded successfully.");
    }

    /*
     * -----------------------------
     * Runtime Getters
     * -----------------------------
     */
     
     
     // -------------------------------------------------------------------------
// Getters (Fabric-compatible)
// -------------------------------------------------------------------------

public static boolean isConfigLoaded() {
    return CONFIG != null;
}

public static int getDebugLevel() {
    return CONFIG.getOrDefault("debugLevel", 0);
}

public static boolean isDebug() {
    return getDebugLevel() > 0;
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
    String actionString = CONFIG.getOrDefault("endOfLifespanAction", "DESTROYED").toUpperCase();
    if (!actionString.equals("DESTROYED") && !actionString.equals("LINGER")) {
        actionString = "DESTROYED";
    }
    try {
        return EndOfLifespanAction.valueOf(actionString);
    } catch (IllegalArgumentException e) {
        return EndOfLifespanAction.DESTROYED;
    }
}

public static String getMobSpawnerLifespanRangesString() {
    return CONFIG.getOrDefault("defMobSpawnerSpawnsRanges",
            "harderspawners:default,200,600;minecraft:pig,0,0;minecraft:cow,0,0;" +
            "minecraft:sheep,0,0;minecraft:parrot,0,0;minecraft:zombie,100,550;minecraft:blaze,0,0;");
}

public static int getSpawnerTextOff() {
    return CONFIG.getOrDefault("spawnerTextOff", 1);
}

public static double getSpawnersExplodePercentage() {
    return CONFIG.getOrDefault("spawnersExplodePercentage", 33.0);
}

// Returns enum for backwards compatibility
public static EndOfLifespanAction getEndOfLifespanAction() {
    try {
        return EndOfLifespanAction.valueOf(endOfLifespanActionString);
    } catch (IllegalArgumentException e) {
        return EndOfLifespanAction.DESTROYED;
    }
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

public static String getMobSpawnerLifespanRangesString() {
    return mobSpawnerDurabilityRangesString;
}
    public static boolean isAddLifespanEnabled() {
        return extraLifespanAmount > 0;
    }

}
