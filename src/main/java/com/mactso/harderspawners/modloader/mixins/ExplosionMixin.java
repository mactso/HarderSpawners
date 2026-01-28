package com.mactso.harderspawners.modloader.mixins;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

@Mixin(Explosion.class)
public class ExplosionMixin {

    // Shadow the private level field so we can access it
    @Shadow
    private Level level; // shadow the private field

    /**
     * Intercepts explosions at the start of finalizeExplosion and removes all spawners
     * from the list of blocks to be destroyed.
     */
    
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void removeSpawnersFromExplosion(CallbackInfo ci) {
        Explosion explosion = (Explosion) (Object) this;
        List<BlockPos> toBlow = explosion.getToBlow();

        Iterator<BlockPos> iterator = toBlow.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (level.getBlockState(pos).is(Blocks.SPAWNER)) {
                iterator.remove();
            }
        }
    }
}