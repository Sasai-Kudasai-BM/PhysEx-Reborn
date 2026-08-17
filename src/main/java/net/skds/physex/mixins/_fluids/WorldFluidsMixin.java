package net.skds.physex.mixins._fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.skds.physex.fluids.FluidUtils;
import net.skds.physex.fluids.PhysExFluidGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class WorldFluidsMixin {

	@Shadow
	public abstract FluidState getFluidState(BlockPos blockPos);

	@Shadow
	public abstract boolean isClientSide();

	@SuppressWarnings("DataFlowIssue")
	@Redirect(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"
			))
	public BlockState setBlock(LevelChunk chunk,
	                           BlockPos blockPos,
	                           BlockState blockState,
	                           int flags
	) {
		FluidState fs = null;
		boolean check = false;
		if (!isClientSide()) {
			if (FluidUtils.checkFlagsForDisplace(flags)) {
				ServerLevel world = (ServerLevel) (Object) this;
				int limit = world.getGameRules().get(PhysExFluidGameRules.FLUID_DISPLACEMENT_LIMIT);
				if (limit > 0) {
					fs = getFluidState(blockPos);
					if (!fs.isEmpty() && fs != blockState.getFluidState()) {
						check = true;
					}
				}
			}
			if ((flags & Block.UPDATE_NEIGHBORS) != 0) {
				FluidUtils.scheduleExtraUpdates((Level) (Object) this, blockPos);
			}
		}
		BlockState oldState = chunk.setBlockState(blockPos, blockState, flags);
		//noinspection ConstantValue
		if (check) {
			FluidUtils.displaceHook((ServerLevel) (Object) this, blockPos, oldState, blockState, blockState.getFluidState(), fs, flags);
		}
		return oldState;
	}

}
