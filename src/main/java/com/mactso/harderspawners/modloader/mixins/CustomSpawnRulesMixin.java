package com.mactso.harderspawners.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mactso.harderspawners.common.logic.SpawnerLightLogic;
import com.mactso.harderspawners.common.utility.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.SpawnData;

/**
 * Overrides CustomSpawnRules to ignore skylight and only check block light for spawner mob spawning.
 */
@Mixin(SpawnData.CustomSpawnRules.class)
public class CustomSpawnRulesMixin {

    /**
     * Overrides CustomSpawnRules to enforce mod-configured light limits for spawner mobs.
     * Delegates the actual check to SpawnerLightLogic.
     */
    @Inject(method = "isValidPosition", at = @At("HEAD"), cancellable = true)
    private void overrideIsValidPosition(BlockPos pos, ServerLevel serverLevel, CallbackInfoReturnable<Boolean> cir) {
        boolean canSpawn = SpawnerLightLogic.canSpawnAt(serverLevel, pos);

        if (!canSpawn) {
            MyUtilities.debugMsg(1, "[CustomSpawnRulesMixin] Spawn blocked at " + pos);
            cir.setReturnValue(false);
        } else {
            MyUtilities.debugMsg(1, "[CustomSpawnRulesMixin] Spawn allowed at " + pos);
            cir.setReturnValue(true);
        }
    }
    
}
