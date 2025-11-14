package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public class LiquidBlockMixin {


	@Inject(method = "fizz", at = @At("HEAD"))
	public void fizz(LevelAccessor levelAccessor, BlockPos blockPos, CallbackInfo ci) {
		FluidUtils.waterEvaporationLava(levelAccessor, blockPos);
	}
}
