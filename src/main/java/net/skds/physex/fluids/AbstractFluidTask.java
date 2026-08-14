package net.skds.physex.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


public abstract class AbstractFluidTask implements Runnable {

	protected static final int MAX_LEVEL = FluidUtils.MAX_LEVEL;
	protected static final int DIR_LEN = 4;// DIRECTIONS.length;

	public final BlockPos pos;
	protected final FlowingFluid fluid;
	protected final ServerLevel world;
	protected final Direction[] randDirs;
	protected final CustomFluidTicks fluidTicks;

	protected AbstractFluidTask(BlockPos pos, FlowingFluid fluid, ServerLevel world) {
		this.pos = pos;
		this.fluid = fluid;
		this.world = world;
		this.randDirs = FluidUtils.randomHorizontal();
		this.fluidTicks = CustomFluidTicks.get(world);
	}

	protected final boolean isThis(FluidState fs) {
		return fs.getType().isSame(fluid);
	}

	protected final void setFluid(BlockPos to, int amount) {
		setFluid(to, getBlockState(to), getFluidState(to), amount, false);
	}

	protected final void setFluid(BlockPos to, BlockState toState, FluidState toFs, int amount, boolean falling) {
		fluidTicks.onBlockUpdate(to, fluid.getTickDelay(world));
		FluidUtils.setFluid(world, to, toState, toFs, fluid, amount, falling);
	}

	protected final int getFluidQuantity(BlockPos p) {
		FluidState fs = getFluidState(p);
		if (isThis(fs)) return fs.getAmount();
		return 0;
	}

	protected final boolean havePath(BlockPos from, Direction dir, boolean wlCare) {
		BlockState fromState = getBlockState(from);
		VoxelShape fromShape = fromState.getCollisionShape(world, from);
		return havePath(from, fromState, fromShape, dir, wlCare);
	}

	protected final boolean havePath(BlockPos from, BlockState fromState, VoxelShape fromShape, Direction dir, boolean wlCare) {
		BlockPos to = from.relative(dir);
		BlockState toState = getBlockState(to);
		if (wlCare && (FluidUtils.checkForWLLimit(fromState, this.fluid) || FluidUtils.checkForWLLimit(toState, this.fluid))) {
			return false;
		}
		VoxelShape toShape = toState.getCollisionShape(world, to);
		if (FluidUtils.isPathBlocked(fromState, fromShape, toState, toShape, dir)) {
			if (!toState.canBeReplaced(fluid)) {
				return false;
			}
			if (FluidUtils.isPathBlocked(fromState, fromShape, null, Shapes.empty(), dir)) {
				return false;
			}
		}
		FluidState toFs = getFluidState(to);
		if (isThis(toFs)) {
			return true;
		}
		return toFs.isEmpty() || canReplace(toFs, to, dir);
	}

	protected final boolean canReplace(FluidState toFs, BlockPos to, Direction dir) {
		return toFs.canBeReplacedWith(world, to, fluid, dir);
		//return false;
	}

	protected final FluidState getFluidState(BlockPos pos) {
		fluidTicks.countBlockRead();
		return world.getFluidState(pos);
	}

	protected final BlockState getBlockState(BlockPos pos) {
		fluidTicks.countBlockRead();
		return world.getBlockState(pos);
	}

	protected final boolean haveFluidOnTop(BlockPos from, BlockState fromState, VoxelShape fromShape) {
		BlockPos to = from.above();
		BlockState toState = getBlockState(to);
		VoxelShape toShape = toState.getCollisionShape(world, to);
		if (FluidUtils.isPathBlocked(fromState, fromShape, toState, toShape, Direction.UP)) {
			if (!toState.canBeReplaced(fluid)) {
				return false;
			}
			if (FluidUtils.isPathBlocked(fromState, fromShape, null, Shapes.empty(), Direction.UP)) {
				return false;
			}
		}
		FluidState toFs = getFluidState(to);
		return isThis(toFs);
	}

}
