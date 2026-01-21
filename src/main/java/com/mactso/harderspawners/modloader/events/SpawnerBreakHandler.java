package com.mactso.harderspawners.modloader.events;

import java.util.List;
import java.util.ListIterator;

import com.mactso.harderspawners.common.logic.SpawnerRevenge;
import com.mactso.harderspawners.common.logic.SpawnerStunLogic;
import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.ModifySpeed;
import net.fabricmc.fabric.api.event.world.ExplosionEvents;

public class SpawnerBreakHandler {

    static int spamLimiter = 0;

    public static void register() {
        // Explosion protection
        ExplosionEvents.DETONATE.register((world, explosion, affectedBlocks, blockEntityMap) -> {
            if (!(world instanceof ServerLevel sLevel)) return;
            ListIterator<BlockPos> iter = affectedBlocks.listIterator(affectedBlocks.size());
            while (iter.hasPrevious()) {
                BlockPos pos = iter.previous();
                if (sLevel.getBlockEntity(pos) instanceof SpawnerBlockEntity) {
                    iter.remove();
                }
            }
        });

        // Block breaking / stun handling
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer sp)) return true; // allow break by default
            if (!(blockEntity instanceof SpawnerBlockEntity sbe)) return true;

            if (sp.isCreative() || MyConfig.getSpawnerMinutesStunned() == 0 || state.getBlock() != Blocks.SPAWNER) {
                SpawnerPositionManager.forgetSpawner((ServerLevel) world, pos);
                return true;
            }

            // Cancel break if stunned
            return !SpawnerStunLogic.processSpawnerStun(sp, (ServerLevel) world, sbe);
        });

        // Break speed adjustment / revenge effects
        ModifySpeed registerSpeedEvent = (world, player, pos, state, originalSpeed) -> {
            if (!(state.getBlock() instanceof SpawnerBlock)) return originalSpeed;

            float newSpeed = originalSpeed;
            if (MyConfig.getSpawnerBreakSpeedModifier() > 0) {
                newSpeed = newSpeed / (1 + MyConfig.getSpawnerBreakSpeedModifier());
            }

            // Optional debug/chat message
            if (player instanceof ServerPlayer sp && (spamLimiter++ % 20 == 0) && MyConfig.getSpawnerTextOff() == 0) {
                MyUtilities.sendChat(sp, "The spawner slowly breaks...", ChatFormatting.DARK_AQUA);
            }

            // Server-side revenge effects
            SpawnerRevenge.doServerSideRevenge(player, pos);

            return newSpeed;
        };
        PlayerBlockBreakEvents.MODIFY_SPEED.register(registerSpeedEvent);
    }
}
