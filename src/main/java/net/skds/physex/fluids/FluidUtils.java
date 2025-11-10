package net.skds.physex.fluids;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skds.lib2.mat.FastMath;
import net.skds.lib2.mat.vec2.Vec2F;
import net.skds.lib2.utils.ArrayUtils;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.fluids.layer.FluidLayer;

@UtilityClass
public class FluidUtils {

	public static final int MAX_LEVEL = 8;
	public static final int FLUID_IN_LEVEL = (int) FluidConstants.BLOCK / MAX_LEVEL;
	public static final int LEVELS_IN_BOTTLE = FastMath.ceil((float) FluidConstants.BOTTLE / FLUID_IN_LEVEL);

	public static final boolean FLUID_CHUNK_LAYER = PhysExBootConfig.INSTANCE.isExtraFluidLayerEnabled();
	public static final PhysExBootConfig.WaterlogPolicy WATERLOG_POLICY = PhysExBootConfig.INSTANCE.getWaterlogPolicy();

	public static final Direction[] WATER_LAVA_DIR = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP};
	public static final FluidState EMPTY = Fluids.EMPTY.defaultFluidState();
	public static final float FLUID_OPEN_HEIGHT = MAX_LEVEL / 9f;
	public static final float ONE_LEVEL = 1f / MAX_LEVEL;
	public static final float MICRO_FLOW_THRESHOLD = 1E-7F;
	public static final float FLOW_THRESHOLD_AXIS = ONE_LEVEL * 2f + MICRO_FLOW_THRESHOLD;
	public static final float FLOW_THRESHOLD = MICRO_FLOW_THRESHOLD;//+ ONE_LEVEL * FastMath.SQRT_2
	public static final float FLOW_THRESHOLD_SQR = FLOW_THRESHOLD * FLOW_THRESHOLD + MICRO_FLOW_THRESHOLD;

	private static final Direction[][] SHUFFLE_H = {
			{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
			{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH},
			{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST},
			{Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH},
			{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH},
			{Direction.EAST, Direction.SOUTH, Direction.NORTH, Direction.WEST},
			{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
			{Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH},
			{Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.EAST},
			{Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
			{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH},
			{Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.EAST},
			{Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH},
			{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
			{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST},
			{Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH},
			{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST},
			{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST},
			{Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH},
			{Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST},
			{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST},
			{Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH},
			{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST},
			{Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST},
	};

	public static Direction getDirection(float x, float z) {
		float absX = Math.abs(x);
		float absZ = Math.abs(z);
		if (absX > absZ) {
			return x > 0 ? Direction.EAST : Direction.WEST;
		} else if (absX < absZ) {
			return z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		if (absX == 0) return null;
		Direction cx = Direction.WEST;
		Direction cz = Direction.NORTH;
		if (x > 0) cx = Direction.EAST;
		if (z > 0) cz = Direction.SOUTH;
		return FastMath.RANDOM.nextBoolean() ? cx : cz;
	}

	public static void handleFluidTick(FlowingFluid ff, ServerLevel world, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
		new ClassicFlowTask(world, blockPos, ff, blockState, fluidState).run();
	}

	public static int getSlopeDistance(FlowingFluid fluid, LevelReader world) {
		return world == null ? 2 : fluid.getSlopeFindDistance(world);
	}

	public static void waterEvaporationLava(LevelAccessor world, BlockPos pos) {
		for (Direction direction : WATER_LAVA_DIR) {
			BlockPos pos2 = pos.relative(direction);
			FluidState fs = world.getFluidState(pos2);
			if (fs.is(FluidTags.WATER)) {
				setFluid(world, pos2, (FlowingFluid) fs.getType(), fs.getAmount() - 1, false);
			}
		}
	}

	public static Vec2F detectSlope(FlowingFluid fluid, BlockGetter world, BlockPos pos, FluidState fs, LevelReader betterWorld) {
		float dx = 0;
		float dz = 0;
		BlockPos pos2;
		BlockPos.MutableBlockPos pos2d = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos pos2l = new BlockPos.MutableBlockPos();
		pos2d.setWithOffset(pos, Direction.DOWN);
		float offset = ONE_LEVEL;
		FluidState fsD = world.getFluidState(pos2d);
		if (fsD.getType().isSame(fluid)) {
			offset = 0;
		}
		int slopeDistance = Math.min(getSlopeDistance(fluid, betterWorld), 4);
		float h = fs.getHeight(world, pos);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			float deltaH = 0.0F;
			pos2 = pos;
			for (int i = 1; i <= slopeDistance; i++) {
				pos2l.set(pos2);
				pos2 = pos2.relative(dir);
				FluidState fs2 = world.getFluidState(pos2);
				if (affectsFlow(fluid, fs2)) {
					if (isPathBlocked(world, pos2l, pos2)) {
						break;
					}
					float h2 = fs2.getOwnHeight();
					if (h2 == 0) {
						pos2d.setWithOffset(pos2, Direction.DOWN);
						if (!isPathBlocked(world, pos2, pos2d)) {
							FluidState fs2d = world.getFluidState(pos2d);
							float d = (h + 1 + offset - fs2d.getHeight(world, pos2d));// / i;
							//if (d > deltaH) deltaH = d;
							deltaH += d / i;
							break;
						} else {
							deltaH += h / i;
						}
					} else if (h2 > 0.0F) {
						float d = (h - h2);// / i;
						//if (d > deltaH) deltaH = d;
						deltaH += d / i;
					}
				}
			}

			if (deltaH > FLOW_THRESHOLD_AXIS || deltaH < -FLOW_THRESHOLD_AXIS) {
				dx += dir.getStepX() * deltaH;
				dz += dir.getStepZ() * deltaH;
			}
		}

		return new Vec2F(dx, dz);
	}

	public static Vec3 getFlow(FlowingFluid fluid, BlockGetter world, BlockPos pos, FluidState fs) {
		Vec2F slope = detectSlope(fluid, world, pos, fs, null);

		float dy = 0;
		if (isFalling(fs)) {
			dy -= 2;
		}

		return new Vec3(slope.xf(), dy, slope.yf());
	}

	public static boolean isPathBlocked(BlockGetter world, BlockPos from, BlockPos to) {
		Direction dir = getDirection(from, to);
		if (dir == null) return false;
		BlockState fromState = world.getBlockState(from);
		VoxelShape fromShape = fromState.getCollisionShape(world, from);
		BlockState toState = world.getBlockState(to);
		VoxelShape toShape = toState.getCollisionShape(world, to);
		return isPathBlocked(fromState, fromShape, toState, toShape, dir);
	}

	@SuppressWarnings("deprecation")
	public static boolean isPathBlocked(BlockState fromState, VoxelShape fromShape, BlockState toState, VoxelShape toShape, Direction dir) {
		// FlowingFluid.canPassThroughWall
		return fromState.blocksMotion()
				|| (toState != null && (toState.blocksMotion()))
				|| (!fromShape.isEmpty() && !toShape.isEmpty() && Shapes.mergedFaceOccludes(fromShape, toShape, dir));
	}

	public static boolean isFalling(FluidState fs) {
		return fs.getValue(FlowingFluid.FALLING);
	}

	private static boolean affectsFlow(FlowingFluid fluid, FluidState fluidState) {
		return fluidState.isEmpty() || fluidState.getType().isSame(fluid);
	}

	public static int modifyTickRate(ServerLevel world, BlockPos pos, Fluid fluid, int time) { // TODO config
		return time / 2;
	}

	@SuppressWarnings("deprecation")
	public static boolean isLiquid(BlockState state) {
		return state.liquid();
	}

	public static Direction[] randomHorizontal() {
		return ArrayUtils.getRandom(SHUFFLE_H);
	}

	public static Direction getDirection(BlockPos from, BlockPos to) {
		return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ(), null);
	}

	public static FluidState getFluidState(FlowingFluid fluid, int level, boolean falling) {
		if (level > MAX_LEVEL || level < 0)
			throw new IllegalStateException("Level " + level);
		if (level == 0 || fluid == Fluids.EMPTY) return EMPTY;
		if (level == MAX_LEVEL) return fluid.getSource(false);
		return fluid.getFlowing(level, false);
	}

	public static boolean isFluidStateOverrided(BlockState blockState) {
		return FLUID_CHUNK_LAYER && blockState.hasProperty(BlockStateProperties.WATERLOGGED);
	}

	public static BlockState applyFluidToBlock(BlockState oldState, FluidState fluidState) {
		if (oldState.isAir() || isLiquid(oldState)) return fluidState.createLegacyBlock();
		if (fluidState.isEmpty()) {
			if (oldState.getFluidState().isEmpty()) return oldState;
			if (oldState.hasProperty(BlockStateProperties.WATERLOGGED)) {
				return oldState.setValue(BlockStateProperties.WATERLOGGED, false);
			}
		}
		if (oldState.hasProperty(BlockStateProperties.WATERLOGGED)) {
			int amount = fluidState.getAmount();
			boolean fill = switch (WATERLOG_POLICY) {
				case ALL_OR_NOTHING -> amount == MAX_LEVEL;
				case FILL_ALWAYS -> amount > 0;
				case FILL_AT_HALF -> amount > MAX_LEVEL / 2;
			};
			return oldState.setValue(BlockStateProperties.WATERLOGGED, fill);
		}
		return fluidState.createLegacyBlock();
	}

	public static void setFluid(LevelAccessor world, BlockPos to, FlowingFluid fluid, int amount, boolean falling) {
		setFluid(world, to, world.getBlockState(to), world.getFluidState(to), fluid, amount, falling);
	}

	public static void setFluid(LevelAccessor world, BlockPos to, BlockState toState, FluidState toFs, FlowingFluid fluid, int amount, boolean falling) {
		FluidState fs = getFluidState(fluid, amount, falling);
		if (fs == toFs) return;
		if (FLUID_CHUNK_LAYER) {
			if (isFluidStateOverrided(toState)) {
				ChunkAccess ca = world.getChunk(to);
				FluidLayer.setFluidState(to, ca, fs);
				((CustomFluidTicks) world.getFluidTicks()).fluidLayerUpdate(ca);
			}
		}
		BlockState bs = applyFluidToBlock(toState, fs);
		if (bs != toState) {
			world.setBlock(to, bs,
					3 | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS,
					1
			);
		}
	}
}
