package com.mactso.harderspawners.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mactso.harderspawners.common.logic.SpawnerMobBuffs;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;

@Mixin(Mob.class)
public abstract class MobSpawnHandlerMixin {

	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	
	private void harderSpawners$onFinalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty,
			MobSpawnType spawnReason, SpawnGroupData spawnData, CallbackInfoReturnable<SpawnGroupData> cir) {
		Mob mob = (Mob) (Object) this;
		SpawnerMobBuffs.applyUndeadSunResistance(mob, spawnReason);
	}
}