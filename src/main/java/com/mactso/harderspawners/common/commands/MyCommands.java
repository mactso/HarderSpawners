package com.mactso.harderspawners.common.commands;


import java.util.List;

import com.mactso.harderspawners.common.logic.SpawnerRegistry;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.adapter.Adapters;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.main.Main;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MyCommands {

    private static final double MAX_LOOK_DISTANCE = 11.0D;
    
    // ---------------------------
    // Command result constants
    // ---------------------------
    public final class CommandResult {
        public static final int NONE = 0;
        public static final int SUCCESS = 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        MyUtilities.debugMsg(0, "Registering " + Main.MODID + " commands.");

        dispatcher.register(
        	    Commands.literal(Main.MODID)
        	        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) // root permission gate
        	        .then(
        	        	    Commands.literal("help")
        	        	        .executes(ctx -> commandHelp(ctx.getSource().getPlayerOrException()))
        	        	) // then(help)
        	        .then(
        	            Commands.literal("info")
        	                .executes(
        	                    ctx -> commandInfo(
        	                        ctx.getSource().getPlayerOrException()
        	                    )
        	                ) // executes(info)
        	        ) // then(info)
        	        .then(
            	            Commands.literal("shownearbyspawners")
            	                .executes(
            	                    ctx -> commandShowNearbyRegisteredSpawners(
            	                        ctx.getSource().getPlayerOrException()
            	                    )
            	                ) // executes(info)
            	        ) // then(info)
        	        .then(
        	                Commands.literal("setinfinitelifespan")
        	                    .executes(ctx -> commandSetInfiniteLifespan(
        	                            ctx.getSource().getPlayerOrException()
        	                    ))
        	            ) // setinfinitelifespan
        	        .then(
        	        	    Commands.literal("setlifespan")
        	        	        .then(
        	        	            Commands.argument("ticks", LongArgumentType.longArg(20L, Long.MAX_VALUE))
        	        	                .executes(ctx -> commandSetLifespan(
        	        	                        ctx.getSource().getPlayerOrException(),
        	        	                        LongArgumentType.getLong(ctx, "ticks")
        	        	                ))
        	        	        ) // ticks
        	        	)
        	); // register(harderspawners)
        	
    }
    

 // -------------------------
 // Command: help
 // -------------------------
 private static int commandHelp(ServerPlayer player) {
     if (player == null) return CommandResult.NONE;

     // Show header
     MyUtilities.sendBoldChat(player, "\n" + Main.MODID + " " + Main.MOD_VERSION + "\n", ChatFormatting.DARK_GREEN);

     // Show command list with usage hint for setlifespan
     MyUtilities.sendChat(player,
         "Available commands:\n"
       + "info - Show info about the spawner you are looking at\n"
       + "setlifespan <ticks> - Set the spawner lifespan to specified ticks (20 to " + Long.MAX_VALUE + "). Example: /harderspawners setlifespan 3000\n"
       + "setinfinitelifespan - Set the spawner to infinite lifespan\n"
       + "shownearbyspawners - List up to 21 nearby registered spawners\n"
       + "help - Show this help message",
         ChatFormatting.GREEN
     );

     return CommandResult.SUCCESS;
 }
 
    // -------------------------
    // Command: info
    // -------------------------
    private static int commandInfo(ServerPlayer player) {
        SpawnerBlockEntity sbe = getLookedAtSpawner(player, MAX_LOOK_DISTANCE);

        if (sbe == null) {
            MyUtilities.sendChat (player, "You see no spawner at all.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        // Get the stats wrapper
        SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
        if (statsWrapper == null) {
        	MyUtilities.sendChat (player, "The Spawner has no entity data.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

    	MyUtilities.sendBoldChat (player, "\n" + Main.MODID + " " + Main.MOD_VERSION + "\n", ChatFormatting.DARK_GREEN);



    	// Show spawner stats
        StringBuilder msg = new StringBuilder();
        
     // Show original entity id (first line of report)
        msg.append("Original EntityId: ")
           .append(statsWrapper.getOriginalEntityId())
           .append("\n");
        
        msg.append("Estimated Spawns Left: ")
        .append(statsWrapper.isInfinite() ? "infinite" : statsWrapper.getEstimatedSpawns())
        .append("\n");
        msg.append("Stunned: ").append(statsWrapper.isStunned()).append("\n");
 
        // Show configured spawn delays
        CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
        int minDelay = SharedUtilityMethods.getIntOrDefault(spawnerTag, "MinSpawnDelay", -1);
        int maxDelay = SharedUtilityMethods.getIntOrDefault(spawnerTag, "MaxSpawnDelay", -1);

        if (minDelay >= 0 && maxDelay >= 0) {
            msg.append("Spawn Delay Range: ")
               .append(minDelay)
               .append("–")
               .append(maxDelay)
               .append(" ticks\n");
        } else {
            msg.append("Spawn Delay Range: <unknown>\n");
        }


    	String hms = getLifespanHMS(statsWrapper) ;       
        msg.append("Lifespan ends: ").append(hms).append(" from now.\n");
        String nextSpawnHMS = getNextSpawnHMS(sbe);
        msg.append("Next spawn in: ").append(nextSpawnHMS).append(" from now.\n");
        // Optional light-level check
        int lightLevel = sbe.getLevel().getRawBrightness(sbe.getBlockPos(), 0);
        if (lightLevel > MyConfig.getHostileSpawnerLightLevel()) {
        	msg.append("Too bright ("+lightLevel+") to spawn right now.\n");
        }        

        MyUtilities.sendChat (player, msg.toString(), ChatFormatting.GREEN);

        return CommandResult.SUCCESS;
    }
    
    // -------------------------
    // Command: setinfinitelifespan
    // -------------------------
    private static int commandSetInfiniteLifespan(ServerPlayer player) {

        SpawnerBlockEntity sbe = getLookedAtSpawner(player, MAX_LOOK_DISTANCE);
        if (sbe == null) {
            MyUtilities.sendChat(player, "You see no spawner.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper =
                SpawnerStatsAdapter.getOrCreateStats(sbe);
        if (statsWrapper == null) {
            MyUtilities.sendChat(player, "Spawner has no valid entity data.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        // Set infinite lifespan explicitly
        statsWrapper.setInfinite(true);
        statsWrapper.setLifespan(Long.MAX_VALUE);

        sbe.setChanged(); // ensure persistence

        MyUtilities.sendChat(
            player,
            "Spawner lifespan set to infinite.",
            ChatFormatting.GREEN
        );

        return CommandResult.SUCCESS;
    }
    

    // -------------------------
    // Command: setlifespan
    // -------------------------
    private static int commandSetLifespan(ServerPlayer player, long lifespanTicks) {

        SpawnerBlockEntity sbe = getLookedAtSpawner(player, MAX_LOOK_DISTANCE);
        if (sbe == null) {
            MyUtilities.sendChat(player, "You see no spawner.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        SpawnerStatsWrapper statsWrapper =
                SpawnerStatsAdapter.getOrCreateStats(sbe);
        if (statsWrapper == null) {
            MyUtilities.sendChat(player, "Spawner has no valid entity data.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        // Explicit finite lifespan
        statsWrapper.setInfinite(false);
        statsWrapper.setLifespan(lifespanTicks);

        sbe.setChanged(); // ensure persistence

        MyUtilities.sendChat(
            player,
            "Spawner lifespan set to "
                + formatSecondsToHMS(lifespanTicks / 20)
                + " (" + lifespanTicks + " ticks).",
            ChatFormatting.GREEN
        );

        return CommandResult.SUCCESS;
    }
    
 // -------------------------
 // Command: showregisteredspawners
 // -------------------------

    private static int commandShowNearbyRegisteredSpawners(ServerPlayer player) {
        if (player == null) return CommandResult.NONE;

        final int maxCount = 21;
        BlockPos playerPos = player.blockPosition();
        ServerLevel level = player.level();

        // Show header
        MyUtilities.sendBoldChat(player, "\n" + Main.MODID + " " + Main.MOD_VERSION + "\n", ChatFormatting.DARK_GREEN);
        MyUtilities.sendChat(player, "List of up to " + maxCount + " nearby registered spawners:", ChatFormatting.GREEN);

        List<BlockPos> nearby = SpawnerRegistry.getNearbySpawners(level, playerPos, 64, maxCount); // 64-block range

        if (nearby.isEmpty()) {
            MyUtilities.sendChat(player, "No spawners registered nearby.", ChatFormatting.YELLOW);
            return CommandResult.NONE;
        }

        // Build comma-separated list
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nearby.size(); i++) {
            BlockPos pos = nearby.get(i);
            sb.append("[").append(pos.getX()).append(",").append(pos.getY()).append(",").append(pos.getZ()).append("]");
            if (i < nearby.size() - 1) sb.append(", ");
        }

        MyUtilities.sendChat(player, sb.toString(), ChatFormatting.GREEN);
        return CommandResult.SUCCESS;
    }
    
    public static String getNextSpawnHMS(SpawnerBlockEntity sbe) {
        int spawnDelayTicks = Adapters.getDelay(sbe.getSpawner());
        long spawnDelaySeconds = Math.max(spawnDelayTicks / 20L, 0); // convert ticks to seconds, avoid negative
        String hms = formatSecondsToHMS(spawnDelaySeconds);
        return hms;
    }
    
    /**
     * Converts a spawner's expiration time into hours:minutes:seconds
     * relative to the chunk's inhabited time.
     *
     * @param sbe the spawner block entity
     * @param expirationTime the absolute spawner expiration time (in ticks)
     * @return formatted string like "01h:23m:45s" or "expired" if already past
     */
    public static String getLifespanHMS(SpawnerStatsWrapper statsWrapper) {
        long remainingTicks = statsWrapper.getLifespan();
        if (remainingTicks <= 0) return "expired";

        long totalSeconds = remainingTicks / 20; // 20 ticks per second
        return formatSecondsToHMS(totalSeconds);
    }

    
    public static String formatSecondsToHMS(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02dh:%02dm:%02ds", hours, minutes, seconds);
    }
    
    // Server-side utility: finds the nearest spawner the player is looking at
    public static SpawnerBlockEntity getLookedAtSpawner(ServerPlayer player, double maxDistance) {
        HitResult hitResult = player.pick(maxDistance, 0.0F, false);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null; // Not looking at a block
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockEntity be = player.level().getBlockEntity(blockHit.getBlockPos());

        if (be instanceof SpawnerBlockEntity sbe) {
            return sbe;
        }

        return null; // Block is not a spawner
    }
    
}
