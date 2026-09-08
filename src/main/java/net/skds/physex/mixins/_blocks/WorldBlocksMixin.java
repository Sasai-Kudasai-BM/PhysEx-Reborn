package net.skds.physex.mixins._blocks;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.skds.physex.blockphysics.BlockPhysicsUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public abstract class WorldBlocksMixin {

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
		if (!isClientSide()) {
			BlockPhysicsUtils.onBlockSet((ServerLevel) (Object) this, chunk, blockPos, oldState, newState, flags);
		}
		return oldState;
	}
}
