package net.skds.physex.mixins._fluids._layer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import net.skds.physex.PhysExUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
public abstract class ServerWorldFluidsLayerMixin extends Level {

	protected ServerWorldFluidsLayerMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
	}

	@Redirect(method = "tickChunk", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
	))
	FluidState tickChunk(BlockState instance,
	                     @Local(argsOnly = true, type = LevelChunk.class) LevelChunk chunk,
	                     @Local(ordinal = 0, type = BlockPos.class) BlockPos pos,
	                     @Local(ordinal = 1, type = int.class) int x,
	                     @Local(ordinal = 2, type = int.class) int y,
	                     @Local(ordinal = 5, type = int.class) int z

	) {
		return chunk.getFluidState(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
	}

	@Override
	public void neighborShapeChanged(Direction direction, BlockPos blockPos, BlockPos blockPos2, BlockState blockState, int i, int j) {
		super.neighborShapeChanged(direction, blockPos, blockPos2, blockState, i, j);
		if ((i & PhysExUtils.NATURAL_BLOCK_FLAG) != 0) return;
		FluidState fs = getFluidState(blockPos);
		if (!fs.isEmpty()) {
			Fluid f = fs.getType();
			scheduleTick(blockPos, f, f.getTickDelay(this));
		}
	}
}
