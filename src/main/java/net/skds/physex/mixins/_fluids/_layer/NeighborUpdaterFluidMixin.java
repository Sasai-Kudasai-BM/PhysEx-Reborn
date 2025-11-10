package net.skds.physex.mixins._fluids._layer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeighborUpdater.class)
public interface NeighborUpdaterFluidMixin {

	@Inject(method = "executeUpdate", at = @At("HEAD"))
	private static void executeUpdate(Level world, BlockState blockState, BlockPos blockPos, Block block, Orientation orientation, boolean bl, CallbackInfo ci) {
		FluidState fs = world.getFluidState(blockPos);
		if (!fs.isEmpty()) {
			Fluid f = fs.getType();
			world.scheduleTick(blockPos, f, f.getTickDelay(world));
		}
	}
}
