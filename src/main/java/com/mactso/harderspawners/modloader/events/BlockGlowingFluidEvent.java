package com.mactso.harderspawners.modloader.events;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mactso.harderspawners.common.logic.BlockAndFluidPlacement;
import com.mactso.harderspawners.common.utility.MyUtilities;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class BlockGlowingFluidEvent {

	// note Boolean, not "boolean". Required by <> map. But Boolean just used as a
	// boolean.
	private static final Map<Fluid, Boolean> fluidBrightnessCache = new ConcurrentHashMap<>();
	private static boolean reflectionFailed = false;
	private static Field BUCKET_CONTENT_FIELD = null;

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

		

			
			if (!(world instanceof ServerLevel serverLevel))
				return InteractionResult.PASS;

			if (!(player instanceof ServerPlayer serverPlayer))
				return InteractionResult.PASS;
			
			

			ItemStack handStack = serverPlayer.getItemInHand(hand);
			if (!(handStack.getItem() instanceof BucketItem bucketItem)) {
				return InteractionResult.PASS;
			}

			if (!(isBrightFluid(bucketItem))) {
				return InteractionResult.PASS;
			}

			BlockPos clickedPos = hitResult.getBlockPos();
			Direction clickedFace = hitResult.getDirection();
			BlockPos placedPos = clickedPos.relative(clickedFace);
			boolean shouldCancel = BlockAndFluidPlacement.handleBucketPlacement(serverPlayer, handStack, clickedPos,
					clickedFace);
			if (shouldCancel) {
			BlockAndFluidPlacement.queuePendingBrightFluid(serverLevel, placedPos);
				// return InteractionResult.FAIL; // but this is ignored by Fabric Minecraft and the
				// lava places anyway.
			}
			// Allow vanilla placement
			return InteractionResult.PASS;
		});
	}

	/**
	 * Returns true if the fluid would emit light (lava or any glowing fluid).
	 * Returns false for dark fluids (light level 0), allowing placement. Note: This
	 * is backed up a blockstate light emission test in PlaceBlock for now. That
	 * event occurs after this check.
	 */
	public static boolean isBrightFluid(BucketItem bucketItem) {

		if (reflectionFailed)
			return false;

		Fluid fluid = getFluid(bucketItem);
		if (fluid == null)
			return false;

		Boolean isBright = fluidBrightnessCache.get(fluid); // check cache
		if (isBright != null)
			return isBright;
		BlockState fluidBlockState = fluid.defaultFluidState().createLegacyBlock();
		isBright = fluidBlockState.getLightEmission() > 0;
		fluidBrightnessCache.put(fluid, isBright); // store in cache

		return isBright;

	}

	/**
	 * Get the Fluid inside a BucketItem.
	 *
	 * @param bucket the BucketItem instance
	 * @return the Fluid contained in the bucket, or null if reflection fails
	 */
	public static Fluid getFluid(BucketItem bucket) {
		if (reflectionFailed || bucket == null)
			return null;

		// --- 1️⃣ Initialize reflective field if necessary ---
		if (BUCKET_CONTENT_FIELD == null) {

			MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

			try {

				// check 1.21.4 production (intermediary) candidate first
				String intermediaryClass = "net.minecraft.class_1755"; // BucketItem
				String intermediaryField = "field_7905"; // content
				String descriptor = "Lnet/minecraft/world/level/material/Fluid;";

				String obfName = resolver.mapFieldName("intermediary", //
						intermediaryClass, //
						intermediaryField, //
						descriptor //
				);

				if (obfName != null) {
					BUCKET_CONTENT_FIELD = BucketItem.class.getDeclaredField(obfName);
					BUCKET_CONTENT_FIELD.setAccessible(true);
                    MyUtilities.debugMsg(1, "[BucketReflectionHelper] Found BucketItem.content via MappingResolver: " + obfName);
				}
			} catch (Exception ignored) {
				// fall through to dev.
			}

			// --- Fallback: deobfuscated name for dev ---
			if (BUCKET_CONTENT_FIELD == null) {
				try {
					BUCKET_CONTENT_FIELD = BucketItem.class.getDeclaredField("content");
					BUCKET_CONTENT_FIELD.setAccessible(true);
                    MyUtilities.debugMsg(1, "[BucketReflectionHelper] Found BucketItem.content via deobfuscated name.");
				} catch (NoSuchFieldException e) {
					reflectionFailed = true;
                    MyUtilities.debugMsg(1, "[BucketReflectionHelper] WARNING: Could not access BucketItem.content.");
					return null;
				}
			}
		}

		// --- 2️⃣ Return the Fluid value ---
		try {
			return (Fluid) BUCKET_CONTENT_FIELD.get(bucket);
		} catch (IllegalAccessException e) {
			reflectionFailed = true;
            MyUtilities.debugMsg(0, "[BucketReflectionHelper] ERROR: Failed to read BucketItem.content field.");
			e.printStackTrace();
			return null;
		}
	}

	/** Returns true if reflection successfully found the content field. */
	public static boolean isWorking() {
		return BUCKET_CONTENT_FIELD != null && !reflectionFailed;
	}
}