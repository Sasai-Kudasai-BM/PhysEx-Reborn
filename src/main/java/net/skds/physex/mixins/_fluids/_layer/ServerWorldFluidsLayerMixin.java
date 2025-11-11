package net.skds.physex.mixins._fluids._layer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
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
						 @Local(name = "blockPos", type = BlockPos.class) BlockPos pos,
						 @Local(name = "j", type = int.class) int x,
						 @Local(name = "o", type = int.class) int y,
						 @Local(name = "k", type = int.class) int z

	) {
		return chunk.getFluidState(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
	}
}
