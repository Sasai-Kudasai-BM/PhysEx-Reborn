package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DoubleHighBlockItem.class)
public class DoubleHighBlockItemFluidMixin {

	@Redirect(method = "placeBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
	))
	boolean placeBlock(Level instance, BlockPos blockPos, BlockState blockState, int i) {
		return false;
	}
}
