package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.Fluid;
import net.skds.physex.fluids.PhysExFluidGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {

	@Inject(method = "findFillableCauldronBelowStalactiteTip", at = @At("HEAD"), cancellable = true)
	private static void findFillableCauldronBelowStalactiteTip(Level level, BlockPos blockPos, Fluid fluid, CallbackInfoReturnable<BlockPos> cir) {
		if (level instanceof ServerLevel sl && !sl.getGameRules().get(PhysExFluidGameRules.DRIPSTONE_FILL_CAULDRON)) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "findStalactiteTipAboveCauldron", at = @At("HEAD"), cancellable = true)
	private static void findStalactiteTipAboveCauldron(Level level, BlockPos blockPos, CallbackInfoReturnable<BlockPos> cir) {
		if (level instanceof ServerLevel sl && !sl.getGameRules().get(PhysExFluidGameRules.DRIPSTONE_FILL_CAULDRON)) {
			cir.setReturnValue(null);
		}
	}
}
