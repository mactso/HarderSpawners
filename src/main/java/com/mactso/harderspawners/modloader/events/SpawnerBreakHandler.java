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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public class SpawnerBreakHandler {
	
	
	static int spamLimiter = 0;
	public static long nextActionTime = 0;
	
	/**
	 * Protects spawner blocks from explosions.
	 * 
	 * Removes any SpawnerBlockEntity positions from the explosion's affected
	 * blocks list, so spawners are not destroyed.
	 * 
	 * Runs only on the server side; client-side events are ignored.
	 * Iterates the affected blocks in reverse to safely remove elements.
	 *
	 * @param event the explosion event being processed
	 */
	@SubscribeEvent
	public void onExplosionDetonate(ExplosionEvent.Detonate event) {
		Level level = event.getLevel();
		if (level.isClientSide())
			return;
		ServerLevel sLevel = (ServerLevel) level;
		List<BlockPos> list = event.getAffectedBlocks();
		//Vec3 vPos = event.getExplosion().center();
		for (ListIterator<BlockPos> iter = list.listIterator(list.size()); iter.hasPrevious();) {
			BlockPos pos = iter.previous();
			if (sLevel.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe) {
				iter.remove();
			}
		}
	}

	// this *only* runs when a block is about to breaks.
	@SubscribeEvent
	public void onBreakBlock(BreakEvent event) {
	    if (!(event.getPlayer() instanceof ServerPlayer sp)) return;

	    BlockPos pos = event.getPos();
	    ServerLevel serverLevel = (ServerLevel) sp.level();
	    BlockEntity be = serverLevel.getBlockEntity(pos);
	    if (!(be instanceof SpawnerBlockEntity sbe))
	    	return;
	    Block targetBlock = serverLevel.getBlockState(pos).getBlock();

	    // If the block will actually break, remove from registry
	    if (sp.isCreative() || MyConfig.getSpawnerMinutesStunned() == 0 || targetBlock != Blocks.SPAWNER) {
	        SpawnerPositionManager.forgetSpawner(serverLevel, pos);
	        return; // allow break
	    }

	    // Otherwise, handle stun logic
	    if (SpawnerStunLogic.processSpawnerStun(sp, serverLevel, sbe)) {
	        event.setCanceled(true);
	    }
	}
	
	@SubscribeEvent
	// this runs almost every tick as a block breaks.
	public void blockBreakSpeed(PlayerEvent.BreakSpeed event) {

		Player player = event.getEntity();
		if ((player == null) || (player.isCreative()))
			return;

		BlockState state = event.getState();
		BlockPos pos = event.getPosition().orElse(null);
		if (!isValidSpawner(state, pos))
			return;
		// this optionally runs on both sides.
		// On the server, change the real digging speed.
		// On the server, inflict revenge
		// On the optional client side, change the visual digging speed.
		doSidedDebugMessage(player);
		SpawnerRevenge.doServerSideRevenge( player, pos); // makes breaking sound effects.
		doBreakSpeedAdjustment(player, event );
	}

	private boolean isValidSpawner(BlockState state, BlockPos pos) {

		if (state == null || pos == null)
			return false;
		return state.getBlock() instanceof SpawnerBlock;
	}

	private void doSidedDebugMessage( Player player) {
		if (MyConfig.getDebugLevel() == 0)
			return;

		String debugSideType = "ServerSide";
		if (player.level().isClientSide()) {
			debugSideType = "ClientSide";
		}
		if (player instanceof ServerPlayer) {
			debugSideType = "ServerSide";

		}
		MyUtilities.debugMsg(1, debugSideType);
	}

	private void doBreakSpeedAdjustment( Player player, PlayerEvent.BreakSpeed event) {
		// potentially both sides
		float baseDestroySpeed = event.getOriginalSpeed();
		float newDestroySpeed = baseDestroySpeed;
		if (MyConfig.getSpawnerBreakSpeedModifier() > 0) {
			newDestroySpeed = newDestroySpeed / (1 + MyConfig.getSpawnerBreakSpeedModifier());
			if (newDestroySpeed > 0) {
				event.setNewSpeed(newDestroySpeed);
				MyUtilities.debugMsg(1,
						"Slowed breaking spawner modifier applied:" + MyConfig.getSpawnerBreakSpeedModifier()
								+ " slowing from " + baseDestroySpeed + " to " + newDestroySpeed + ".");
			}
		}
		doOptionalMessage(player);
	}

	// This only runs if installed on both sides or on the integrated server.
	private void doOptionalMessage(Player player) {
		if (player instanceof ServerPlayer sp) {
			if ((spamLimiter++) % 20 == 0 && (MyConfig.getSpawnerTextOff() == 0)) {
				MyUtilities.sendChat(sp, "The spawner slowly breaks...", ChatFormatting.DARK_AQUA);
			}
		}
	}



}
