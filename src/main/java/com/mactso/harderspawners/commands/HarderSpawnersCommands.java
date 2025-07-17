package com.mactso.harderspawners.commands;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.Utility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class HarderSpawnersCommands {
	String subcommand = "";
	String value = "";
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("harderspawners").requires((source) -> 
			{
				return source.hasPermission(3);
			}
		)
		.then(Commands.literal("setdebugLevel").then(
				Commands.argument("setdebugLevel", IntegerArgumentType.integer(0,2)).executes(ctx -> {
					return setDebugLevel(IntegerArgumentType.getInteger(ctx, "setdebugLevel"));
					// return 1;
			}
			)
			)
			)
		.then(Commands.literal("info").executes(ctx -> {
					ServerPlayer serverPlayerEntity = (ServerPlayer) ctx.getSource().getEntity();
					Level level = serverPlayerEntity.level();

					String chatMessage = "\n HarderSpawners Info";
					Utility.sendChat(serverPlayerEntity, chatMessage, ChatFormatting.DARK_GREEN);
		            chatMessage = 
  	            		    "  Debug Level...................: " + MyConfig.getDebugLevel()
		            		+ "\n  Player Range.......................: " + MyConfig.getRequiredPlayerRange()
		            		+ "\nSpawner"
		            		+ "\n  Mob Cap.......................: " + MyConfig.getMaxNearbyEntities()
		            		+ "\n  Break Speed Modifier...: 1/" + MyConfig.getSpawnerBreakSpeedModifier() 
		            		+ "\n  Stun Minutes........: " + MyConfig.getSpawnerMinutesStunned() 
		            		+ "\n  Revenge Level..................: " + MyConfig.getSpawnerRevengeLevel()
		            		+ "\n  Durability Item........: " + MyConfig.getDurabilityItem()  

		            		;
					Utility.sendChat(serverPlayerEntity, chatMessage, ChatFormatting.GREEN);
		            return 1;
			}
			)
			)		
		);

	}
	
	
	public static int setDebugLevel (int newDebugLevel) {
		MyConfig.setDebugLevel(newDebugLevel); 
		return 1;
	}
	

}
