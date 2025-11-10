package net.skds.physex.mixins._fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason Completely new way of ticking fluids
	 */
	@Overwrite
	public Vec3 getFlow(BlockGetter blockGetter, BlockPos blockPos, FluidState fluidState) {
		return FluidUtils.getFlow((FlowingFluid) (Object) this, blockGetter, blockPos, fluidState);
	}

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason Completely new way of ticking fluids
	 */
	@Overwrite
	public void tick(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
		FluidUtils.handleFluidTick((FlowingFluid) (Object) this, serverLevel, blockPos, blockState, fluidState);
	}
}
