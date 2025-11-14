package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(DoublePlantBlock.class)
public class DoubleBlockFluidMixin {

	@Redirect(method = "preventDropFromBottomPart", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
	))
	private static boolean preventDropFromBottomPart(Level instance, BlockPos blockPos, BlockState blockState, int i) {
		return instance.setBlock(blockPos, instance.getFluidState(blockPos).createLegacyBlock(), i);
	}
}
