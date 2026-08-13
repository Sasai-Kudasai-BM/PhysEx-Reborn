package net.skds.physex.mixins._fluids._layer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.skds.physex.fluids.layer.FluidLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityLayerMixin extends BlockEntity {

	@Unique
	private FluidState movedFluidState;

	public PistonMovingBlockEntityLayerMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	void loadAdditional(ValueInput valueInput, CallbackInfo ci) {
		this.movedFluidState = valueInput.read("fluidState", FluidState.CODEC).orElse(Fluids.EMPTY.defaultFluidState());
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	void saveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
		if (this.movedFluidState != null) {
			valueOutput.store("fluidState", FluidState.CODEC, this.movedFluidState);
		}
	}

	@Inject(method = "setLevel", at = @At("TAIL"))
	public void setLevel(Level level, CallbackInfo ci) {
		if (this.movedFluidState == null) {
			BlockPos pos = getBlockPos();
			LevelChunk chunk = level.getChunkAt(pos);
			FluidState fs = FluidLayer.getFluidState(pos, chunk);
			if (fs != null) {
				this.movedFluidState = fs;
			}
		}
	}


	@Inject(method = "finalTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
			ordinal = 0
	))
	public void finalTick(CallbackInfo ci) {
		FluidState fs = this.movedFluidState;
		if (fs != null && level != null && !fs.isEmpty()) {
			BlockPos pos = getBlockPos();
			LevelChunk chunk = level.getChunkAt(pos);
			FluidLayer.setFluidState(pos, chunk, fs);
		}
	}

}
