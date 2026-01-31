package com.mactso.harderspawners.modloader.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.harderspawners.modloader.events.ExplosionDetonateEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;

/**
 * Prevents spawner blocks from being destroyed by server-side explosions.
 *
 * Injects into {@link ServerExplosion} during block interaction and
 * removes spawner positions from the affected block list before any
 * destruction, drops, or decay logic is applied.
 *
 * Applies only to server-side explosions.
 */
@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {

    /** The level the explosion occurs in (never null) */
    @Shadow
    private ServerLevel level;

    @Inject(
        method = "interactWithBlocks",
        at = @At("HEAD")
    )
    private void harderspawners$onInteractWithBlocks(
            List<BlockPos> list,
            CallbackInfo ci
    ) {
        ExplosionDetonateEvent.protectBlocks(this.level, list);
    }
}
