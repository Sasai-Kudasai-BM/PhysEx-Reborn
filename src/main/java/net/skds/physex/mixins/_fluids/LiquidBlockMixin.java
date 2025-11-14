package net.skds.physex.mixins._fluids;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin extends Block {

	@Shadow
	@Final
	private List<FluidState> stateCache;

	public LiquidBlockMixin(Properties properties) {
		super(properties);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(FlowingFluid flowingFluid, Properties properties, CallbackInfo ci) {
		this.stateCache.removeLast();
		this.stateCache.add(flowingFluid.getSource(true));
		for (int i = 1; i < 8; i++) {
			this.stateCache.add(flowingFluid.getFlowing(8 - i, true));
		}
		this.registerDefaultState(this.stateDefinition.any().setValue(LiquidBlock.LEVEL, 0));
	}

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason fix of mojang stupidity
	 */
	@Overwrite
	public FluidState getFluidState(BlockState blockState) {
		int i = blockState.getValue(LiquidBlock.LEVEL);
		return this.stateCache.get(i);
	}
}
