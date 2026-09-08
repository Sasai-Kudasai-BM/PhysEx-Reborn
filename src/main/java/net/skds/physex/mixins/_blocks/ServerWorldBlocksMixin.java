package net.skds.physex.mixins._blocks;

import net.minecraft.server.level.ServerLevel;
import net.skds.physex.blockphysics.BlockPhysicsManager;
import net.skds.physex.blockphysics.BlockPhysicsManagerGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerWorldBlocksMixin implements BlockPhysicsManagerGetter {

	@Unique
	private final BlockPhysicsManager blockPhysicsManager = new BlockPhysicsManager((ServerLevel) (Object) this);

	@Inject(method = "runBlockEvents", at = @At("HEAD"))
	private void runBlockEvents(CallbackInfo ci) {
		blockPhysicsManager.tick();
	}

	@Override
	public BlockPhysicsManager physEx$getBlockPhysicsManager() {
		return blockPhysicsManager;
	}
}
