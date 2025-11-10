package net.skds.physex.mixins._fluids;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FluidState;
import net.skds.physex.PhysEx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {

	@Inject(method = "getFluidState", at = @At("HEAD"))
	public void getFluidState(CallbackInfoReturnable<FluidState> cir) {
		PhysEx.LOGGER.warn("Deprecated method call \"getFluidState\"");
	}
}
