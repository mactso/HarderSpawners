package com.mactso.harderspawners.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.harderspawners.modloader.spawnerstorage.ISpawnerStats;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin implements ISpawnerStats {

    @Unique
    private SpawnerStatsStorage spawnerStatsStorage;

    @Override
    public SpawnerStatsStorage getSpawnerStatsStorage() {
        return this.spawnerStatsStorage;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(BlockPos pos, BlockState state, CallbackInfo ci) {
        this.spawnerStatsStorage = new SpawnerStatsStorage();
    }

    // Inject after the ValueOutput saveAdditional method
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void saveSpawnerStats(ValueOutput output, CallbackInfo ci) {
        if (spawnerStatsStorage != null) {
            spawnerStatsStorage.serialize(output);
        }
    }

    // Inject after the ValueInput loadAdditional method
    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void loadSpawnerStats(ValueInput input, CallbackInfo ci) {
        if (spawnerStatsStorage == null) {
            spawnerStatsStorage = new SpawnerStatsStorage();
        }
        spawnerStatsStorage.deserialize(input);
    }
}


