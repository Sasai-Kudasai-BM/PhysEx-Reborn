package net.skds.physex.fluids;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
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
import net.skds.physex.PhysEx;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.PhysExGameRules;
import net.skds.physex.fluids.layer.FluidLayer;

import java.util.ArrayDeque;
import java.util.function.BiFunction;

@UtilityClass
public class FluidUtils {

	public static final TagKey<Block> FLUID_NOT_FRIENDLY_TAG = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, "fluid_not_friendly")
	);
	public static final TagKey<Block> FLUID_SOLID_TAG = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, "fluid_solid")
	);

	public static final TagKey<Block> FLAMMABLE_TAG = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, "flammable")
	);

	public static final int DISPLACE_FLAG = 1 << PhysExBootConfig.INSTANCE.getFluidDisplaceFlagBitOffset();

	public static final int BURN_TEMP = 500 + 273;
	public static final int MAX_LEVEL = 8;
	public static final int FLUID_IN_LEVEL = (int) FluidConstants.BLOCK / MAX_LEVEL;
	public static final int LEVELS_IN_BOTTLE = FastMath.ceil((float) FluidConstants.BOTTLE / FLUID_IN_LEVEL);

	public static final boolean FLUID_CHUNK_LAYER = PhysExBootConfig.INSTANCE.isExtraFluidLayerEnabled();
	public static final PhysExBootConfig.WaterlogPolicy WATERLOG_POLICY = PhysExBootConfig.INSTANCE.getWaterlogPolicy();


	public static final BiFunction<Level, BlockPos, FluidState> FS_GETTER = Level::getFluidState;
	public static final Direction[] HORIZONTAL = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
	public static final Direction[] WATER_LAVA_DIR = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP};
	public static final FluidState EMPTY = Fluids.EMPTY.defaultFluidState();
	public static final float FLUID_OPEN_HEIGHT = MAX_LEVEL / 9f;
	public static final float ONE_LEVEL = 1f / MAX_LEVEL;
	public static final float MICRO_FLOW_THRESHOLD = 1E-7F;
	public static final float FLOW_SLOPE_THRESHOLD_AXIS = ONE_LEVEL * 2f + MICRO_FLOW_THRESHOLD;
	public static final float FLOW_THRESHOLD_AXIS = ONE_LEVEL + MICRO_FLOW_THRESHOLD;
	public static final float FLOW_THRESHOLD = MICRO_FLOW_THRESHOLD;
	public static final float FLOW_THRESHOLD_SQR = FLOW_THRESHOLD * FLOW_THRESHOLD + MICRO_FLOW_THRESHOLD;

	public static final float BOAT_FLOATABILITY = 0.02f;
	public static final float BOAT_FLOAT_OFFSET = -0.15f;

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
	private static final Direction[][] SHUFFLE_FLUID_PRIORITY;
	private static final Direction[][] SHUFFLE_INVERSE_PRIORITY;

	static {
		Direction[][] shuffle = new Direction[SHUFFLE_H.length][];
		Direction[][] shuffleInv = new Direction[SHUFFLE_H.length][];
		for (int i = 0; i < shuffle.length; i++) {
			Direction[] layer = new Direction[6];
			System.arraycopy(SHUFFLE_H[i], 0, layer, 1, 4);
			layer[0] = Direction.DOWN;
			layer[5] = Direction.UP;
			shuffle[i] = layer;
			layer = layer.clone();
			layer[0] = Direction.UP;
			layer[5] = Direction.DOWN;
			shuffleInv[i] = layer;
		}
		SHUFFLE_FLUID_PRIORITY = shuffle;
		SHUFFLE_INVERSE_PRIORITY = shuffleInv;
	}

	public static Direction[] randomHorizontal() {
		return ArrayUtils.getRandom(SHUFFLE_H);
	}

	public static Direction[] randomAllPriority() {
		return ArrayUtils.getRandom(SHUFFLE_FLUID_PRIORITY);
	}

	public static Direction[] randomAllPriorityInverted() {
		return ArrayUtils.getRandom(SHUFFLE_INVERSE_PRIORITY);
	}


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

	public static boolean pistonHook(ServerLevel world, PistonStructureResolver pistonStructureResolver, BlockPos blockPos, Direction pistonDirection) {
		ObjectOpenHashSet<BlockPos> blacklist = new ObjectOpenHashSet<>(8, .5f);
		Direction pushDirection = pistonStructureResolver.getPushDirection();
		for (BlockPos pos : pistonStructureResolver.getToPush()) {
			blacklist.add(pos);
			blacklist.add(pos.relative(pushDirection));
		}
		Object2ObjectOpenHashMap<BlockPos, FluidState> displaceStates = new Object2ObjectOpenHashMap<>(8, .5f);
		for (BlockPos pos : pistonStructureResolver.getToDestroy()) {
			blacklist.add(pos);
			FluidState fs = world.getFluidState(pos);
			if (!fs.isEmpty() && fs.getType() instanceof FlowingFluid) {
				displaceStates.put(pos, fs);
			}
		}
		if (displaceStates.isEmpty()) return true;
		Object2ObjectOpenHashMap<BlockPos, FluidState> positions = new Object2ObjectOpenHashMap<>(displaceStates.size() * 2, .5f);
		BiFunction<Level, BlockPos, FluidState> filter = (w, bp) -> {
			if (blacklist.contains(bp)) return null;
			FluidState fs = positions.get(bp);
			if (fs == null) {
				return w.getFluidState(bp);
			}
			return fs;
		};
		for (var e : displaceStates.entrySet()) {
			FluidState fs = e.getValue();
			FlowingFluid fluid = (FlowingFluid) fs.getType();
			FluidDistribution space = findSpaceForFluidAround(world, e.getKey(), fluid, fs.getAmount(), filter);
			if (!space.isEmpty()) {
				if (space.remainingFluid() > 0) return false;
				for (var e2 : space.occupied().object2IntEntrySet()) {
					FluidState fs2 = getFluidState(fluid, e2.getIntValue(), false);
					positions.put(e2.getKey(), fs2);
				}
			}
		}
		if (!positions.isEmpty()) for (var e : positions.entrySet()) {
			setFluidState(world, e.getKey(), e.getValue());
		}
		return true;
	}

	public static void handleFluidTick(FlowingFluid ff, ServerLevel world, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
		new ClassicFlowTask(world, blockPos, ff, blockState, fluidState).run();
	}

	public static int getSlopeDistance(FlowingFluid fluid, LevelReader world) {
		return world == null ? 2 : fluid.getSlopeFindDistance(world);
	}

	public static void waterEvaporationLava(LevelAccessor world, BlockPos pos) {
		if (world instanceof ServerLevel sl) {
			int el = sl.getGameRules().getInt(PhysExGameRules.WATER_HOT_EVAPORATION);
			if (el == 0) return;
			for (Direction direction : WATER_LAVA_DIR) {
				BlockPos pos2 = pos.relative(direction);
				FluidState fs = world.getFluidState(pos2);
				if (fs.is(FluidTags.WATER)) {
					setFluid(sl, pos2, (FlowingFluid) fs.getType(), Math.max(0, fs.getAmount() - el), false);
				}
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

			if (deltaH > FLOW_SLOPE_THRESHOLD_AXIS || deltaH < -FLOW_SLOPE_THRESHOLD_AXIS) {
				dx += dir.getStepX() * deltaH;
				dz += dir.getStepZ() * deltaH;
			}
		}

		return new Vec2F(dx, dz);
	}

	public static Vec3 getFlow(FlowingFluid fluid, BlockGetter world, BlockPos pos, FluidState fs) {

		float dx = 0;
		float dz = 0;
		BlockPos posD = pos.below();
		float offset = 0;
		FluidState fsD = world.getFluidState(posD);
		if (fsD.getType().isSame(fluid)) {
			offset = ONE_LEVEL;
		}
		float h = fs.getOwnHeight();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			float deltaH = 0.0F;
			BlockPos pos2 = pos.relative(dir);
			FluidState fs2 = world.getFluidState(pos2);

			if (affectsFlow(fluid, fs2) && !isPathBlocked(world, pos, pos2)) {
				float h2 = fs2.getOwnHeight();
				if (h2 == 0) {
					BlockPos pos2d = pos2.below();
					FluidState fs2d = world.getFluidState(pos2d);
					if (affectsFlow(fluid, fs2d) && !isPathBlocked(world, pos2, pos2d)) {
						deltaH = (h + 1 - offset - fs2d.getOwnHeight());
					} else {
						deltaH = h;
					}
				} else if (h2 > 0.0F) {
					deltaH = (h - h2);
				}
			}

			if (deltaH > FLOW_THRESHOLD_AXIS || deltaH < -FLOW_THRESHOLD_AXIS) {
				dx += dir.getStepX() * deltaH;
				dz += dir.getStepZ() * deltaH;
			}
		}

		float dy = 0;
		if (isFalling(fs)) {
			dy -= 2;
		}

		return new Vec3(dx, dy, dz);
	}

	public static boolean isPathBlocked(BlockGetter world, BlockPos from, BlockPos to, BlockState toState, VoxelShape toShape) {
		Direction dir = getDirection(from, to);
		if (dir == null) return false;
		BlockState fromState = world.getBlockState(from);
		VoxelShape fromShape = fromState.getCollisionShape(world, from);
		return isPathBlocked(fromState, fromShape, toState, toShape, dir);
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

	public static boolean isPathBlocked(BlockState fromState, VoxelShape fromShape, BlockState toState, VoxelShape toShape, Direction dir) {

		if ((fromState != null && fromState.is(FLUID_SOLID_TAG)) || (toState != null && toState.is(FLUID_SOLID_TAG)))
			return true;

		if (fromShape.isEmpty() && toShape.isEmpty()) return false;
		if (fromShape == Shapes.block() && fromState != null && fromState.hasProperty(BlockStateProperties.WATERLOGGED))
			fromShape = Shapes.empty();
		if (toState != null && toShape == Shapes.block() && toState.hasProperty(BlockStateProperties.WATERLOGGED))
			toShape = Shapes.empty();
		return Shapes.mergedFaceOccludes(fromShape, toShape, dir);
	}

	public static boolean isFalling(FluidState fs) {
		return fs.getValue(FlowingFluid.FALLING);
	}

	private static boolean affectsFlow(FlowingFluid fluid, FluidState fluidState) {
		return fluidState.isEmpty() || fluidState.getType().isSame(fluid);
	}

	public static int modifyTickRate(int time) { // TODO config
		return time / 2;
	}

	@SuppressWarnings("deprecation")
	public static boolean isLiquid(BlockState state) {
		return state.liquid();
	}

	public static Direction getDirection(BlockPos from, BlockPos to) {
		return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ(), null);
	}

	public static FluidState getFluidState(FlowingFluid fluid, int level, boolean falling) {
		if (level > MAX_LEVEL || level < 0)
			throw new IllegalStateException("Level " + level);
		if (level == 0 || fluid == Fluids.EMPTY) return EMPTY;
		if (level == MAX_LEVEL) return fluid.getSource(falling);
		return fluid.getFlowing(level, falling);
	}

	public static boolean isFluidStateOverrided(BlockState blockState) {
		return FLUID_CHUNK_LAYER && !isLiquid(blockState) && !blockState.isAir()
				&& !(blockState.getBlock() instanceof LiquidBlockContainer && !blockState.hasProperty(BlockStateProperties.WATERLOGGED));
	}

	public static boolean isFlammable(BlockState blockState) {
		if (blockState.ignitedByLava() || blockState.is(FLAMMABLE_TAG)) return true;
		return FlammableBlockRegistry.getDefaultInstance().get(blockState.getBlock()).getBurnChance() > 0;
	}

	public static boolean canHandleFluid(BlockState blockState, FlowingFluid fluid) {
		if (fluid == Fluids.EMPTY) return true;
		if (blockState.is(FLUID_NOT_FRIENDLY_TAG)) return false;
		if (isFlammable(blockState)) {
			int temp = FluidVariantAttributes.getTemperature(FluidVariant.of(fluid.getSource()));
			return temp < BURN_TEMP;
		}
		//if (blockState.is(FLUID_FRIENDLY_TAG)) return true;
		return true;
	}

	public static BlockState applyFluidToBlock(BlockState oldState, FluidState fluidState, boolean override) {
		if (oldState.isAir() || isLiquid(oldState)) return fluidState.createLegacyBlock();
		if (fluidState.isEmpty()) {
			if (oldState.getFluidState().isEmpty()) return oldState;
			if (fluidState.getType().isSame(Fluids.WATER) && oldState.hasProperty(BlockStateProperties.WATERLOGGED)) {
				return oldState.setValue(BlockStateProperties.WATERLOGGED, false);
			}
		}
		if (fluidState.getType().isSame(Fluids.WATER) && oldState.hasProperty(BlockStateProperties.WATERLOGGED)) {
			int amount = fluidState.getAmount();
			boolean fill = switch (WATERLOG_POLICY) {
				case ALL_OR_NOTHING -> amount == MAX_LEVEL;
				case FILL_ALWAYS -> amount > 0;
				case FILL_AT_HALF -> amount > MAX_LEVEL / 2;
			};
			return oldState.setValue(BlockStateProperties.WATERLOGGED, fill);
		}
		if (override) {
			return oldState;
		}
		return fluidState.createLegacyBlock();
	}

	public static boolean checkFlagsForDisplace(int flags) {
		if ((flags & DISPLACE_FLAG) != 0 || (flags & Block.UPDATE_MOVE_BY_PISTON) != 0) return true;
		return (flags & Block.UPDATE_IMMEDIATE) != 0 && (flags & Block.UPDATE_NEIGHBORS) != 0;
	}

	public static void placeFluid(ServerLevel world, FlowingFluid fluid, FluidDistribution distribution) {
		for (var e : distribution.occupied().object2IntEntrySet()) {
			setFluid(world, e.getKey(), fluid, e.getIntValue(), false);
		}
	}

	public static FluidDistribution findSpaceForFluid(ServerLevel world,
	                                                  BlockPos to,
	                                                  FlowingFluid fluid,
	                                                  int amount,
	                                                  BiFunction<Level, BlockPos, FluidState> fsGetter
	) {
		if (amount < 1 || fluid == Fluids.EMPTY) return FluidDistribution.EMPTY;

		int occupied0 = 0;
		FluidState fs2 = fsGetter.apply(world, to);
		if (fs2 != null) {
			boolean same = fs2.getType().isSame(fluid);
			if (same || fs2.isEmpty() || fs2.canBeReplacedWith(world, to, fluid, null)) {
				int fsl = same ? fs2.getAmount() : 0;
				int capacity = MAX_LEVEL - fsl;
				int toPut = Math.min(amount, capacity);
				if (toPut > 0) {
					amount -= toPut;
					occupied0 = fsl + toPut;
				}
			}
		}
		FluidDistribution ffs = findSpaceForFluidAround(world, to, fluid, amount, fsGetter);
		if (!ffs.isEmpty()) {
			ffs.occupied().put(to, occupied0);
			return ffs;
		}

		return FluidDistribution.single(to, occupied0, amount);
	}

	public static FluidDistribution findSpaceForFluidAround(ServerLevel world,
	                                                        BlockPos to,
	                                                        FlowingFluid fluid,
	                                                        int amount,
	                                                        BiFunction<Level, BlockPos, FluidState> fsGetter
	) {
		LongOpenHashSet visited = new LongOpenHashSet(amount * 6, 0.5f);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(amount * 6);
		Object2IntOpenHashMap<BlockPos> map = new Object2IntOpenHashMap<>(16, .5f);
		visited.add(to.asLong());
		queue.offer(to);
		BlockPos next;
		int limit = 128;
		int i = 0;
		while ((next = queue.poll()) != null) {
			if (i++ > limit) break;
			for (Direction dir : randomAllPriority()) {
				BlockPos p2 = next.relative(dir);
				long lp = p2.asLong();
				if (visited.contains(lp)) continue;
				BlockState state2 = world.getBlockState(p2);
				VoxelShape shape2 = state2.getCollisionShape(world, p2);
				if (to == next ? !isPathBlocked(null, Shapes.empty(), state2, shape2, dir) : !isPathBlocked(world, next, p2, state2, shape2)) {
					visited.add(lp);
					FluidState fs2 = fsGetter.apply(world, p2);
					if (fs2 == null) {
						if (amount > 0) queue.add(p2);
						continue;
					}
					boolean same = fs2.getType().isSame(fluid);
					if (!same && !fs2.isEmpty() && !fs2.canBeReplacedWith(world, p2, fluid, dir)) continue;
					int fsl = same ? fs2.getAmount() : 0;
					int capacity = MAX_LEVEL - fsl;
					int toPut = Math.min(amount, capacity);
					if (toPut > 0) {
						amount -= toPut;
						map.put(p2, fsl + toPut);
					}
					if (amount > 0) queue.add(p2);
				}
			}
		}
		return new FluidDistribution(map, amount);
	}

	public static int placeFluidAround(ServerLevel world, BlockPos to, FlowingFluid fluid, int amount) {
		FluidDistribution distribution = findSpaceForFluidAround(world, to, fluid, amount, FS_GETTER);
		placeFluid(world, fluid, distribution);
		return distribution.remainingFluid();
	}

	public static void displaceHook(ServerLevel world, BlockPos to, BlockState oldState, BlockState newState, FluidState newFs, FluidState correctFs, int flags) {
		if (newFs.isEmpty() && newState.isAir() && isLiquid(oldState)) {
			return;
		}
		if (!correctFs.isEmpty() && correctFs.getType() instanceof FlowingFluid fluid) {
			if (newFs.isEmpty() && applyFluidToBlock(newState, correctFs, true).getFluidState() == correctFs) {
				setFluidState(world, to, newState, newFs, correctFs);
			} else {
				int rem = placeFluidAround(world, to, fluid, correctFs.getAmount());
				setFluid(world, to, fluid, rem, false);
			}
		}
	}

	public static void scheduleExtraUpdates(Level world, BlockPos pos, Fluid fluid) {
		BlockPos posU = pos.above();
		for (Direction direction : HORIZONTAL) {
			world.scheduleTick(posU.relative(direction), fluid, fluid.getTickDelay(world));
		}
	}

	public static void scheduleExtraUpdates(Level world, BlockPos pos) {
		BlockPos posU = pos.above();
		for (Direction direction : HORIZONTAL) {
			BlockPos pos2U = posU.relative(direction);
			FluidState fs = world.getFluidState(pos2U);
			if (!fs.isEmpty()) {
				Fluid fluid = fs.getType();
				world.scheduleTick(pos2U, fluid, fluid.getTickDelay(world));
			}
		}
	}

	public static void setFluidState(Level world, BlockPos to, BlockState toState, FluidState toFs, FluidState fs) {
		Fluid f0 = fs.isEmpty() ? toFs.getType() : fs.getType();
		if (f0 instanceof FlowingFluid flowingFluid) {
			setFluid(world, to, toState, toFs, flowingFluid, fs, true);
		}
	}

	public static void setFluidState(Level world, BlockPos to, FluidState fs) {
		FluidState toFs = world.getFluidState(to);
		Fluid f0 = fs.isEmpty() ? toFs.getType() : fs.getType();
		if (f0 instanceof FlowingFluid flowingFluid) {
			setFluid(world, to, world.getBlockState(to), toFs, flowingFluid, fs, true);
		}
	}

	public static void setFluid(Level world, BlockPos to, FlowingFluid fluid, int amount, boolean falling) {
		setFluid(world, to, world.getBlockState(to), world.getFluidState(to), fluid, getFluidState(fluid, amount, falling), true);
	}

	public static void setFluid(Level world, BlockPos to, BlockState toState, FluidState toFs, FlowingFluid fluid, int amount, boolean falling, boolean update) {
		setFluid(world, to, toState, toFs, fluid, getFluidState(fluid, amount, falling), update);
	}

	public static void setFluid(Level world, BlockPos to, BlockState toState, FluidState toFs, FlowingFluid fluid, int amount, boolean falling) {
		setFluid(world, to, toState, toFs, fluid, getFluidState(fluid, amount, falling), true);
	}

	public static void setFluid(Level world, BlockPos to, BlockState toState, FluidState toFs, FlowingFluid fluid, FluidState fs, boolean update) {
		if (fs == toFs) return;
		int flags = Block.UPDATE_CLIENTS
				| Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
		if (update) {
			flags |= Block.UPDATE_NEIGHBORS;
		} else {
			flags |= Block.UPDATE_KNOWN_SHAPE;
		}

		boolean override = isFluidStateOverrided(toState) && canHandleFluid(toState, fluid);
		if (override) {
			ChunkAccess ca = world.getChunk(to);
			FluidLayer.setFluidState(to, ca, fs);
			if (!fs.isEmpty()) {
				world.scheduleTick(to, fluid, fluid.getTickDelay(world));
			}
			if (!world.isClientSide()) {
				CustomFluidTicks.get(world).fluidLayerUpdate(ca, to);
				scheduleExtraUpdates(world, to, fluid);
			}
		}

		BlockState bs = applyFluidToBlock(toState, fs, override);
		if (bs != toState || override) {
			boolean placed = false;
			if (override || ((bs.isAir() || isLiquid(bs)) && !toState.isAir() && !isLiquid(toState))) {
				boolean destroy = true;
				if (toState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
					placed = liquidBlockContainer.placeLiquid(world, to, toState, fs.isEmpty() ? fs : fluid.getSource().defaultFluidState());
					if (placed) {
						BlockState vanillaBs = world.getBlockState(to);
						toState = vanillaBs;
						if (vanillaBs.getFluidState() != fs) {
							bs = applyFluidToBlock(vanillaBs, fs, override);
							placed = false;
						}
						destroy = false;
					}
				}
				if (destroy && !override && !toState.isAir()) {
					fluid.beforeDestroyingBlock(world, to, toState);
				}
			}
			if (!placed && bs != toState) {
				world.setBlock(to, bs, flags, 16);
			}
		}
	}
}
