package net.skds.physex.client.mixins._fluids._layer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCompiler.class)
public class SectionCompilerFluidMixin {

	@Redirect(
			method = "compile",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
			))
	FluidState compile(BlockState instance,
					   @Local(ordinal = 0, name = "blockPos3", type = BlockPos.class) BlockPos blockPos,
					   @Local(argsOnly = true, ordinal = 0, type = RenderSectionRegion.class) RenderSectionRegion renderSectionRegion
	) {
		return renderSectionRegion.getFluidState(blockPos);
	}
}
