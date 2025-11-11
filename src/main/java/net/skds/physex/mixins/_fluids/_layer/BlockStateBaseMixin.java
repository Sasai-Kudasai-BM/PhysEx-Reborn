package net.skds.physex.mixins._fluids._layer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {

	//@Inject(method = "getFluidState", at = @At("HEAD"))
	//public void getFluidState(CallbackInfoReturnable<FluidState> cir) {
	//	PhysEx.LOGGER.warn("Deprecated method call \"getFluidState\"");
	//}

	@Inject(method = "updateNeighbourShapes(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V", at = @At("HEAD"))
	public void updateNeighbourShapes(LevelAccessor world, BlockPos blockPos, int i, int j, CallbackInfo ci) {
		if (world.isClientSide()) return;
		FluidState fs = world.getFluidState(blockPos);
		if (!fs.isEmpty()) {
			Fluid f = fs.getType();
			world.scheduleTick(blockPos, f, f.getTickDelay(world));
		}
	}
}
