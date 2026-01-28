package com.mactso.harderspawners.modloader.events;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class BlockGlowingFluidEvent {

	// note Boolean, not "boolean".  Required by <> map.  But Boolean just used as a boolean.
    private static final Map<Fluid, Boolean> fluidBrightnessCache = new ConcurrentHashMap<>();
    private static boolean reflectionFailed = false;
    private static Field BUCKET_CONTENT_FIELD = null;


	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

			if (!(world instanceof ServerLevel serverLevel))
				return InteractionResult.PASS;

			ItemStack handStack = player.getItemInHand(hand);

			if (handStack.is(Items.LAVA_BUCKET))
				return InteractionResult.FAIL;

			if (!(handStack.getItem() instanceof BucketItem bucketItem))
				return InteractionResult.PASS;

			// 🔒 Pre-check: cancel BEFORE placement
			if (isBrightFluid(bucketItem)) {
				return InteractionResult.FAIL; // cancels bucket placement
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
    private static boolean isBrightFluid(BucketItem bucketItem) {
    	
        if (reflectionFailed) return false;

        Fluid fluid = getFluid(bucketItem);
        if (fluid == null) return false;

        Boolean isBright = fluidBrightnessCache.get(fluid); // check cache
        if (isBright != null) return isBright;
        BlockState fluidBlockState = fluid.defaultFluidState().createLegacyBlock();
        isBright = fluidBlockState.getLightEmission() > 0;
        fluidBrightnessCache.put(fluid, isBright); // store in cache

        return isBright;
        
    }

	/**
	 * Get the Fluid inside a BucketItem using reflection. Lazily initializes the
	 * Field on first access. Logs a warning if the field cannot be accessed.
	 */
	public static Fluid getFluid(BucketItem bucket) {

		if (BUCKET_CONTENT_FIELD == null) {
			try {
				BUCKET_CONTENT_FIELD = BucketItem.class.getDeclaredField("content");
				BUCKET_CONTENT_FIELD.setAccessible(true);
			} catch (NoSuchFieldException e) {
				reflectionFailed = true;
				System.err
						.println("[BucketReflectionHelper] WARNING: 'content' field in BucketItem cannot be accessed.");
				return null;
			}
		}

		try {
			return (Fluid) BUCKET_CONTENT_FIELD.get(bucket);
		} catch (IllegalAccessException e) {
			reflectionFailed = true;
			System.err.println("[BucketReflectionHelper] WARNING: Failed to read 'content' field from BucketItem.");
			return null;
		}

	}
}