package net.skds.physex.mixins._fluids._layer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.skds.physex.fluids.FluidUtils;
import net.skds.physex.fluids.layer.FluidLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class WorldChunkFluidMixin {

	@Shadow
	@Final
	Level level;

	@Inject(method = "setBlockState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;",
			ordinal = 0
	))
	void setBlockState(BlockPos blockPos,
					   BlockState blockState,
					   int i,
					   CallbackInfoReturnable<BlockState> cir,
					   @Local(name = "blockState2", type = BlockState.class) BlockState oldState
	) {
		if (!level.isClientSide() && !FluidUtils.isFluidStateOverrided(blockState)) {
			FluidLayer.resetFluidState(blockPos, (LevelChunk) (Object) this);
		}
	}

	@Inject(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at = @At("HEAD"), cancellable = true)
	void getFluidState(int x, int y, int z, CallbackInfoReturnable<FluidState> cir) {
		FluidState fs = FluidLayer.getFluidState(x, y, z, (LevelChunk) (Object) this);
		if (fs != null) cir.setReturnValue(fs);
	}
}
