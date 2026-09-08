package net.skds.physex.mixins._fluids;

import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public abstract class WorldFluidsMixin {

	@Shadow
	public abstract boolean isClientSide();

	@SuppressWarnings("DataFlowIssue")
	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At(value = "STORE"),
			ordinal = 1
	)
	public BlockState setBlock(BlockState oldState,
	                           @Local(ordinal = 0) LevelChunk chunk,
	                           @Local(argsOnly = true) BlockPos blockPos,
	                           @Local(argsOnly = true) BlockState newState,
	                           @Local(argsOnly = true, ordinal = 0) int flags
	) {
		FluidState fs = null;
		boolean check = false;
		if (!isClientSide()) {
			if (oldState != null && FluidUtils.checkFlagsForDisplace(flags)) {
				ServerLevel world = (ServerLevel) (Object) this;
				int limit = world.getGameRules().get(PhysExFluidGameRules.FLUID_DISPLACEMENT_LIMIT);
				if (limit > 0) {
					fs = FluidUtils.getFsOrVanilla(blockPos, chunk, oldState);
					if (!fs.isEmpty() && fs != newState.getFluidState()) {
						check = true;
					}
				}
			}
			if ((flags & Block.UPDATE_NEIGHBORS) != 0) {
				FluidUtils.scheduleExtraUpdates((Level) (Object) this, blockPos);
			}
			//noinspection ConstantValue
			if (check) {
				FluidUtils.displaceHook((ServerLevel) (Object) this, blockPos, oldState, newState, newState.getFluidState(), fs, flags);
			}
		}
		return oldState;
	}
}
