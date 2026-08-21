package net.skds.physex.mixins._blocks;

import net.minecraft.world.level.block.state.BlockState;
import net.skds.physex.blockphysics.BlockStatePhysicsAccessor;
import net.skds.physex.blockphysics.BlockStatePhysicsHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockState.class)
public class BlockStateBlocksMixin implements BlockStatePhysicsAccessor {

	@Unique
	private BlockStatePhysicsHolder blockPhysicsHolder;

	@Override
	public BlockStatePhysicsHolder physEx$getHolder() {
		return blockPhysicsHolder;
	}

	@Override
	public void physEx$setHolder(BlockStatePhysicsHolder holder) {
		this.blockPhysicsHolder = holder;
	}
}
