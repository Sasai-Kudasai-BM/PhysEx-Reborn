package net.skds.physex.client.mixins._fluids;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.skds.physex.client.fluids.ClientFluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LiquidBlockRenderer.class)
public class FluidRendererMixin {

	/*
	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F"
			),
			slice = @Slice(
					from = @At(value = "INVOKE",
							target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F",
							ordinal = 1
					),
					to = @At(value = "INVOKE",
							target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;calculateAverageHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;FFFLnet/minecraft/core/BlockPos;)F"
					))
	)
	float tess0(LiquidBlockRenderer instance,
				BlockAndTintGetter blockAndTintGetter,
				Fluid fluid,
				BlockPos pos2,
				BlockState blockState,
				FluidState fluidState
	) {
		return 0;
	}
	 */

	@Redirect(method = "tesselate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;calculateAverageHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;FFFLnet/minecraft/core/BlockPos;)F"
			)
	)
	float calculateAverageHeight(LiquidBlockRenderer instance,
								 BlockAndTintGetter world,
								 Fluid fluid,
								 float self,
								 float g, float h,
								 BlockPos offsetPos,
								 @Local(argsOnly = true, type = BlockPos.class) BlockPos startPos
								 //@Local(argsOnly = true, type = BlockState.class) BlockState blockState,
								 //@Local(argsOnly = true, type = FluidState.class) FluidState fluidState
	) {
		return ClientFluidUtils.calculateAverageHeight(world, fluid, self, g, h, offsetPos, startPos);//, blockState, fluidState);
	}

}
