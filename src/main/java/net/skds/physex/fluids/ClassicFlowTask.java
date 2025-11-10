package net.skds.physex.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skds.lib2.mat.vec2.Vec2F;
import net.skds.physex.PhysEx;

public class ClassicFlowTask extends AbstractFluidTask {

	private final BlockState state;
	private final FluidState initialFluidState;
	private final VoxelShape shape;
	private final int[] dirPotential = new int[DIR_LEN];

	private int level;

	public ClassicFlowTask(ServerLevel world, BlockPos pos, FlowingFluid fluid, BlockState bs, FluidState fs) {
		super(pos, fluid, world);
		this.initialFluidState = fs;
		this.state = bs;
		this.shape = this.state.getShape(world, pos);
		this.level = this.initialFluidState.getAmount();
	}

	@Override
	public void run() {
		fluidTicks.countTask();
		flowRun();
	}

	private void flowRun() {
		boolean moved = false;
		int delta = canFlow(Direction.DOWN);
		if (delta > 0) {
			if (this.level == delta && delta > 1) {
				delta--;
			}
			flow(Direction.DOWN, delta);
			moved = true;
		}
		if (this.level == 0) return;
		//BlockPos posD = pos.below();
		//boolean freeDown = moved || havePath(Direction.DOWN);
		//if (freeDown) for (int i = 0; i < DIR_LEN; i++) {
		//	Direction dir = randDirs[i];
		//	delta = canFlow(posD, dir, this.level, true);
		//	if (delta > 0) {
		//		flow(posD.relative(dir), dir, delta);
		//		moved = true;
		//		break;
		//	}
		//}

		//*
		int n = 0;
		for (int i = 0; i < DIR_LEN; i++) {
			Direction dir = randDirs[i];
			delta = canFlow(dir) / 2;
			if (delta > 0) {
				dirPotential[i] = delta;
				n++;
			}
		}
		if (n != 0) {
			for (int i = 0; i < DIR_LEN; i++) {
				int d = dirPotential[i] / n;
				if (d == 0) {
					continue;
				}

				if (d > this.level) {
					d = this.level;
				}
				Direction dir = randDirs[i];
				flow(dir, d);
				moved = true;
			}
			for (int i = 0; i < DIR_LEN; i++) {
				Direction dir = randDirs[i];
				int d = canFlow(dir) / 2;
				if (d > 0) {
					flow(dir, d);
					moved = true;
					break;
				}
			}
		}

		// */
		if (!moved) {
			if (!haveFluidOnTop(pos, state, shape)) {
				Vec2F slope = FluidUtils.detectSlope(fluid, world, pos, initialFluidState, world);
				if (slope.lengthSquared() > FluidUtils.FLOW_THRESHOLD_SQR) {
					slopeFlow(slope);
					return;
				}
				equalize();
			}
		}
	}

	private int canFlow(BlockPos from, Direction dir, int limit, boolean fromAbove) {
		BlockState fromState = getBlockState(from);
		VoxelShape fromShape = fromState.getCollisionShape(world, from);
		return Math.min(canFlow(from, fromState, fromShape, dir, fromAbove), limit);
	}

	private void flow(Direction dir, int amount) {
		flow(pos, state, pos.relative(dir), dir, amount);
	}

	private void flow(BlockPos to, Direction dir, int amount) {
		flow(pos, state, to, dir, amount);
	}

	private void flow(BlockPos from, BlockState fromState, BlockPos to, Direction dir, int amount) {
		FluidState toFs = getFluidState(to);
		this.level -= amount;
		setFluid(from, fromState, getFluidState(from), this.level, dir == Direction.DOWN);

		if (isThis(toFs)) {
			int toLevel = toFs.getAmount() + amount;
			setFluid(to, getBlockState(to), toFs, toLevel, dir == Direction.DOWN);
		} else if (toFs.isEmpty() || canReplace(toFs, to, dir)) {
			setFluid(to, getBlockState(to), toFs, amount, dir == Direction.DOWN);
		} else {
			PhysEx.LOGGER.warn("Impossible " + to);
		}
	}

	private void slopeFlow(Vec2F slope) {

		Direction dir = FluidUtils.getDirection(slope.xf(), slope.yf());
		if (dir == null) return;
		int delta = canFlow(dir);
		if (delta > 0) {
			delta /= 2;
			if (delta == 0) delta = 1;
			flow(dir, delta);
		} else {
			equalize();
		}
	}

	private void equalize() {
		if (level < 2) return;
		fluidTicks.scheduleEqualization(pos, fluid, world);
	}


	private int canFlow(Direction dir) {
		return Math.min(canFlow(pos, state, shape, dir, dir == Direction.DOWN), this.level);
	}

	private int canFlow(BlockPos from, BlockState fromState, VoxelShape fromShape, Direction dir, boolean fromAbove) {
		BlockPos to = from.relative(dir);
		BlockState toState = getBlockState(to);
		VoxelShape toShape = toState.getCollisionShape(world, to);
		if (FluidUtils.isPathBlocked(fromState, fromShape, toState, toShape, dir)) {
			if (!toState.canBeReplaced(fluid)) {
				return 0;
			}
			if (FluidUtils.isPathBlocked(fromState, fromShape, null, Shapes.empty(), dir)) {
				return 0;
			}
		}
		//if (fromState.hasProperty(BlockStateProperties.WATERLOGGED)) return 0;
		//if (toState.hasProperty(BlockStateProperties.WATERLOGGED)) return 0;
		FluidState toFs = getFluidState(to);
		if (isThis(toFs)) {
			int level2 = toFs.getAmount();
			if (fromAbove) {
				return MAX_LEVEL - level2;
			}
			return this.level - level2;
		}
		if (toFs.isEmpty() || canReplace(toFs, to, dir)) return MAX_LEVEL;
		return 0;
	}

	private boolean havePath(Direction dir) {
		return havePath(pos, state, shape, dir);
	}


}
