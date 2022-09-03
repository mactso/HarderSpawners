package com.mactso.harderspawners.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class Utility {
	
	final static int TWO_SECONDS = 40;
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void debugMsg (int level, String dMsg) {

		if (MyConfig.getDebugLevel() > level-1) {
			LOGGER.info("L"+level + ":" + dMsg);
		}
		
	}

	public static void debugMsg (int level, BlockPos pos, String dMsg) {

		if (MyConfig.getDebugLevel() > level-1) {
			LOGGER.info("L"+level+" ("+pos.getX()+","+pos.getY()+","+pos.getZ()+"): " + dMsg);
		}
		
	}

	public static void sendBoldChat(Player p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal (chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(ChatFormatting.DARK_GREEN));
		p.sendSystemMessage(component);
	}	
	

	public static void sendChat(Player p, String chatMessage, ChatFormatting textColor) {

        MutableComponent component = Component.literal (chatMessage);
		component.setStyle(component.getStyle().withColor(ChatFormatting.GREEN));
        p.sendSystemMessage(component);
	}
	
	public static void updateEffect(LivingEntity e, int amplifier,  MobEffect mobEffect, int duration) {
		MobEffectInstance ei = e.getEffect(mobEffect);
		if (amplifier == 10) {
			amplifier = 20;  // player "plaid" speed.
		}
		if (ei != null) {
			if (amplifier > ei.getAmplifier()) {
				e.removeEffect(mobEffect);
			} 
			if (amplifier == ei.getAmplifier() && ei.getDuration() > 10) {
				return;
			}
			if (ei.getDuration() > 10) {
				return;
			}
			e.removeEffect(mobEffect);			
		}
		e.addEffect(new MobEffectInstance(mobEffect, duration, amplifier, true, true));
		return;
	}
	
	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier) {
		boolean isBaby = false;
		return populateEntityType(et, level, savePos, range, modifier, isBaby);
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier, boolean isBaby) {
		boolean persistant = false;
		return populateEntityType(et, level, savePos, range, modifier, persistant, isBaby);
	}
	
	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier, boolean persistant, boolean isBaby) {
		int numZP;
		Mob e;
		numZP = level.random.nextInt(range) - modifier;
		if (numZP < 0)
			return false;
		for (int i = 0; i <= numZP; i++) {
			if (et == EntityType.PHANTOM) {
				e = (Mob) et.spawn(level, null, null, null, savePos.north(2).west(2), MobSpawnType.SPAWNER, true, true);
			} else {
				e = (Mob) et.spawn(level, null, null, null, savePos.north(2).west(2), MobSpawnType.NATURAL, true, true);
			}
			if (persistant) {
				e.setPersistenceRequired();
			}
			if (et == EntityType.ZOMBIFIED_PIGLIN) {
				e.setAggressive(true);
			}
			e.setBaby(isBaby);
		}
		return true;
	}
	
	public static boolean isOutside(BlockPos pos, ServerLevel serverLevel) {
		return serverLevel.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, pos) == pos;
	}

}
