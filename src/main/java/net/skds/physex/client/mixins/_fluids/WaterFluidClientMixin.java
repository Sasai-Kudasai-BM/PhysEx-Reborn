package net.skds.physex.client.mixins._fluids;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WaterFluid.class)
public class WaterFluidClientMixin {
	
	@Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0))
	public int animateTick(RandomSource random, int i,
	                       @Local(argsOnly = true) Level level,
	                       @Local(argsOnly = true) BlockPos blockPos,
	                       @Local(argsOnly = true) FluidState fluidState
	) {
		if (fluidState.getFlow(level, blockPos).lengthSqr() < FluidUtils.FLOW_THRESHOLD_SQR) {
			return -1;
		}
		return random.nextInt(i);
	}
}
