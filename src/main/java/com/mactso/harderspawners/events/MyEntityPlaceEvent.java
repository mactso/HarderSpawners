package com.mactso.harderspawners.events;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber() 
public class MyEntityPlaceEvent {
	@SubscribeEvent
	public static boolean bucket(FillBucketEvent event) {
		boolean result = MyConfig.CONTINUE_EVENT;
		Level level = (Level) event.getLevel();
		if (level.isClientSide()) {
			return result;
		}
		ServerLevel slevel = (ServerLevel) level;

		ItemStack stack = event.getEmptyBucket();
		BlockPos placedPos = null;

		if (!(stack.getItem() instanceof BucketItem))
			return result;
		
		BucketItem b = (BucketItem) stack.getItem();
		if (b.getFluid().getFluidType().getLightLevel() == 0)
			return result;

		if (!(event.getTarget().getType() == Type.BLOCK))
			return result;

		BlockHitResult br = (BlockHitResult) event.getTarget();
		placedPos = br.getBlockPos().relative(br.getDirection());
		if (placedPos == null)
			return result;

		if (!(ServerTickHandler.isSpawnerNearby(slevel, placedPos)))
			return result;

		slevel.playSound(null, placedPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f, 0.25f);
		doLavaPlacementFailParticles(slevel, placedPos, br);

		return MyConfig.CANCEL_EVENT;
	}

	@SubscribeEvent
	public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		
		if (event.getLevel().isClientSide())
			return;
		ServerLevel sLevel = (ServerLevel) event.getLevel();
		if (sLevel.getRandom().nextInt(100) > MyConfig.getDestroyLightPercentage())
			return;

		BlockState placedBlockState = event.getPlacedBlock();
		Block placedBlock = placedBlockState.getBlock();
		BlockPos placedPos = event.getPos();

		if ( SharedUtilityMethods.adjustBlockBrightness(sLevel, placedBlockState, placedBlock, placedPos) == 0)
			return;
		if (sLevel.getMaxLocalRawBrightness(placedPos) == 15)
			return;
		if (!(ServerTickHandler.isSpawnerNearby(sLevel, placedPos)))
			return;

		sLevel.destroyBlock(placedPos, true);

	}



	private static void doLavaPlacementFailParticles(ServerLevel sLevel, BlockPos pos, BlockHitResult br) {

		RandomSource rand = sLevel.getRandom();
		Direction d = br.getDirection();
		double offset = 0.0d;
		if (d == Direction.DOWN)
			offset = +0.70d;
		int numParticles = 27;
		for (int j = 0; j < numParticles; ++j) {
			double vx = (0.3d * rand.nextDouble()) - 0.15d;
			double vz = (0.3d * rand.nextDouble()) - 0.15d;
			Vec3 rfv = new Vec3(vx, 0.1 + rand.nextDouble() / 32, vz);
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.15d + offset;
			double z = (double) pos.getZ() + rand.nextDouble();
			SimpleParticleType particles = ParticleTypes.SMOKE;
			sLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}
	}
}
