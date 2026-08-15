package net.skds.physex.mixins._fluids._layer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.skds.physex.fluids.CustomFluidTicks;
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
	                   int flags,
	                   CallbackInfoReturnable<BlockState> cir
	) {
		if (!level.isClientSide() && (flags & Block.UPDATE_MOVE_BY_PISTON) == 0 && !FluidUtils.isFluidStateOverrided(blockState)) {
			LevelChunk _this = (LevelChunk) (Object) this;
			if (FluidLayer.resetFluidState(blockPos, _this)) {
				CustomFluidTicks.get(level).fluidLayerUpdate(_this, blockPos);
			}
		}
	}

	@Inject(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at = @At("HEAD"), cancellable = true)
	void getFluidState(int x, int y, int z, CallbackInfoReturnable<FluidState> cir) {
		FluidState fs = FluidLayer.getFluidState(x, y, z, (LevelChunk) (Object) this);
		if (fs != null) cir.setReturnValue(fs);
	}
}
