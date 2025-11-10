package net.skds.physex.client.mixins._fluids._layer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LiquidBlockRenderer.class)
public class FluidRendererMixin {

	@Redirect(
			method = "getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)F",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
			))
	FluidState getHeight(BlockState instance,
						 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
						 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos);
	}

	@Redirect(
			method = "getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
			))
	FluidState getHeight2(BlockState instance,
						  @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
						  @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.above());
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 0
			))
	FluidState tess0(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.DOWN));
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 1
			))
	FluidState tess1(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.UP));
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 2
			))
	FluidState tess2(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.NORTH));
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 3
			))
	FluidState tess3(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.SOUTH));
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 4
			))
	FluidState tess4(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.WEST));
	}

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
					ordinal = 5
			))
	FluidState tess5(BlockState instance,
					 @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos,
					 @Local(argsOnly = true, ordinal = 0, type = BlockAndTintGetter.class) BlockAndTintGetter blockAndTintGetter
	) {
		return blockAndTintGetter.getFluidState(blockPos.relative(Direction.EAST));
	}
}
