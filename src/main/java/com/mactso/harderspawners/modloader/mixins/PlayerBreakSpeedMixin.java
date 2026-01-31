package com.mactso.harderspawners.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mactso.harderspawners.common.logic.SpawnerLightLogic;
import com.mactso.harderspawners.common.logic.SpawnerRevenge;
import com.mactso.harderspawners.common.logic.SpecialEffects;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Mixin applied to {@link BlockBehaviour.BlockStateBase} to intercept block
 * destruction progress calculation.
 *
 * <p>
 * This mixin applies a short-duration Mining Fatigue effect to players attempting
 * to break mob spawners, thereby indirectly slowing the break speed without
 * modifying the return value of {@code getDestroyProgress()}.
 * </p>
 *
 * <p>
 * Injection occurs at {@code RETURN}, meaning the current tick's destroy progress
 * is not affected. The applied status effect influences subsequent mining ticks.
 * </p>
 *
 * <p>
 * This mixin is server-side only and safely ignores all client-side invocations.
 * </p>
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PlayerBreakSpeedMixin {
	/**
	 * Injects after {@code BlockStateBase#getDestroyProgress} returns.
	 *
	 * <p>
	 * When a server player is mining a spawner block, Mining Fatigue is applied
	 * every tick to reduce effective break speed. Additional feedback is provided
	 * when the spawner is in a "stunned" state.
	 * </p>
	 *
	 * @param player the player attempting to break the block
	 * @param level  a {@link BlockGetter} view of the world (not authoritative for BEs)
	 * @param pos    the block position being mined
	 * @param cir    the original return value of destroy progress (unused)
	 */
	@Inject(method = "getDestroyProgress", at = @At("RETURN"))
	private void harderspawners$applyMiningFatigue(Player player, BlockGetter level, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(2, pos, "PlayerBreakSpeedMixin call");

		// Server-side only
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		if (!(serverPlayer.level() instanceof ServerLevel serverLevel))
			return;
		
		// 'this' is the BlockStateBase instance this mixin targets
		BlockState state = (BlockState) (Object) this;
		if (!state.is(Blocks.SPAWNER))
			return;

		BlockEntity be = serverLevel.getBlockEntity(pos);
		if (!(be instanceof SpawnerBlockEntity sbe))
			return;
		
		// Skip spawners that got destroyed mid ticks
		if (sbe.isRemoved())
			return;

		// Provide periodic feedback when the spawner is stunned.
		if (serverLevel.getGameTime() % 20 == 0) {
			SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
			if ((statsWrapper != null ) && (statsWrapper.isStunned())) { // null if breaking empty spawner
				// this is a hack to avoid client side mixin.
				serverLevel.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);
			}
		}

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, pos, "Applying mining fatigue to spawner breaker");
		// Mining Fatigue I (amplifier = 0)
		int amplifier = MyConfig.getSpawnerBreakSpeedModifier();
		int durationTicks = 20; // very short, refreshed while mining

		// Apply or refresh the Mining Fatigue effect to slow destruction of the spawner
		MyUtilities.updateEffect(serverPlayer, amplifier, MobEffects.MINING_FATIGUE, durationTicks);
		if (serverLevel.getGameTime()%17 == 0) {
			RandomSource rand = serverLevel.getRandom();
			SpecialEffects.doSpawnerBreakingEffects(pos, serverPlayer, serverLevel, sbe, rand);
			SpawnerLightLogic.destroyLightingNearSpawner(sbe);
		}

		SpawnerRevenge.doServerSideRevenge(serverPlayer, pos);

	}
}
