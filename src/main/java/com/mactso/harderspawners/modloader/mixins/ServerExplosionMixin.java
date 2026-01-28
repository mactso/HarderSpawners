package com.mactso.harderspawners.modloader.mixins;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.harderspawners.common.utility.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {

    /* ------------------------------------------------------------
     * Shadows
     * ------------------------------------------------------------ */

    /** The level the explosion occurs in (never null) */
    @Shadow
    private ServerLevel level;

    /* ------------------------------------------------------------
     * Injection
     * ------------------------------------------------------------ */

    /**
     * Removes spawner blocks from the explosion destruction list.
     *
     * Called immediately before blocks are interacted with and dropped.
     *
     * @param list Mutable list of block positions that will be destroyed.
     *             Possible contents:
     *             - Any block within explosion radius
     *             - May include air-adjacent positions
     *             - Never null
     */
    @Inject(
        method = "interactWithBlocks",
        at = @At("HEAD")
    )
    private void harderspawners$removeSpawnersFromExplosion(
            List<BlockPos> list,
            CallbackInfo ci
    ) {
        Iterator<BlockPos> iterator = list.iterator();
        //if (MyConfig.isDebug())
        	MyUtilities.debugMsg(0,"ServerExplosionMixin: Removing spawners from list with " + list.size() + " entries." );
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();

            // Possible states:
            // - Blocks.SPAWNER
            // - Trial spawner variants
            // - Any other block affected by explosion
            if (this.level.getBlockState(pos).is(Blocks.SPAWNER)) {
                iterator.remove();
            	MyUtilities.debugMsg(0,"ServerExplosionMixin: Removed a spawner from the list ." );
            }
        }
    }
}
