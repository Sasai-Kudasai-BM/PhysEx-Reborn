package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.path.StablePathValue;

import java.util.Arrays;

import static net.skds.physex.blockphysics.BlockPhysicsUtils.*;

public final class BlockPhysicsChain {

	private static final boolean DEBUG = PhysEx.DEBUG;

	public static final int LONG_BEAM_FLAG = 1;
	public static final int ARC_FLAG = 2;

	public static final int ARC_REQ_MASK_TOP = 1 << 31;

	private final BlockPos.MutableBlockPos cursor1 = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos cursor2 = new BlockPos.MutableBlockPos();

	private final BlockPos.MutableBlockPos unwindCursor1 = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos unwindCursor2 = new BlockPos.MutableBlockPos();

	private final LongOpenHashSet visited = new LongOpenHashSet(16, .5f);
	private final Long2IntOpenHashMap carryDirection = new Long2IntOpenHashMap(16, .5f);
	private final Long2IntOpenHashMap requireSupport = new Long2IntOpenHashMap(16, .5f);
	private final LongArrayList unwindQueue = new LongArrayList(16);
	private long[] nodes = new long[8];
	private byte[] nodeDirections = new byte[nodes.length];
	private byte[] nodeFlags = new byte[nodes.length];
	private short[] nodeDistance = new short[nodes.length];
	private final LongOpenHashSet nodeSet = new LongOpenHashSet(16, .5f);
	private int head = -1;

	private final BlockPhysicsManager manager;
	private final ServerLevel world;
	private final BlockPos startPos;
	private final long startPosL;

	private boolean success = false;

	public BlockPhysicsChain(BlockPhysicsManager manager, BlockPos startPos, BlockState startState, BlockPhysicsData physics) {
		this.startPos = startPos;
		this.startPosL = startPos.asLong();
		this.manager = manager;
		this.world = manager.world;
		pushNode(startPos.asLong(), physics.getDirMask(), 0, 0);
		visited.add(startPosL);
	}

	private void pushNode(long pos, int dirMask, int distance, int flags) {
		if (dirMask == 0 || !nodeSet.add(pos)) return;
		int h = head + 1;
		if (h >= this.nodes.length) {
			this.nodes = Arrays.copyOf(this.nodes, this.nodes.length * 2);
			this.nodeDirections = Arrays.copyOf(this.nodeDirections, this.nodes.length);
			this.nodeFlags = Arrays.copyOf(this.nodeFlags, this.nodes.length);
			this.nodeDistance = Arrays.copyOf(this.nodeDistance, this.nodes.length);
		}
		head = h;
		this.nodes[h] = pos;
		this.nodeDistance[h] = (short) distance;
		this.nodeFlags[h] = (byte) flags;
		this.nodeDirections[h] = (byte) dirMask;
	}

	public void tick() {
		long pos;
		int dist;
		int mask;
		int flags;
		int i = 1000;
		while (head >= 0 && !success) {
			if (--i <= 0) {
				System.err.println("Tragedy");
				return;
			}
			pos = nodes[head];
			dist = nodeDistance[head];
			flags = nodeFlags[head];
			mask = nodeDirections[head--];
			if (!nodeSet.remove(pos)) {
				continue;
			}

			checkBlock(pos, mask, dist, flags);
		}
		if (!success && DEBUG) {
			cursor1.setWithOffset(startPos, Direction.DOWN);
			while (visited.contains(cursor1.asLong())) {
				cursor1.move(Direction.DOWN);
			}
			cursor1.move(Direction.UP);
			BlockPhysicsDebug.debug(cursor1, Blocks.RED_STAINED_GLASS.defaultBlockState());
		}
	}

	private boolean checkRequiredSupport(long pos, Direction direction) {
		int value = requireSupport.get(pos);
		if ((value & ARC_REQ_MASK_TOP) != 0) {
			if (DEBUG) {
				BlockPhysicsDebug.debug(BlockPos.of(pos), Blocks.BLUE_STAINED_GLASS.defaultBlockState());
			}
			unwindCursor2.set(pos).move(0, -1, 0);
			pos = unwindCursor2.asLong();
			value = requireSupport.get(pos);
		}
		if (value == 0) return true;
		int dirMask = 1 << direction.ordinal();
		int dirMask2 = dirMask << 8;
		int value2 = value & ~(dirMask | dirMask2);
		if (value != value2) {
			value = value2;
			requireSupport.put(pos, value);
		}
		return (value & 0xff) == 0 || (value & 0xff00) == 0;
	}

	private void requireSupport(long pos, int mask1, int mask2) {
		int value = (mask1 & 0xff) | ((mask2 & 0xff) << 8);
		requireSupport.putIfAbsent(pos, value);
	}

	private void requireTopSupport(long pos) {
		requireSupport.put(pos, ARC_REQ_MASK_TOP);
		//if (DEBUG) {
		//	BlockPhysicsDebug.debug(BlockPos.of(pos), Blocks.LIME_STAINED_GLASS.defaultBlockState());
		//}
	}

	private void stablePathCandidate(long pos, Direction value) {
		int mask = carryDirection.get(pos);
		if (mask != 0) {
			mask <<= StablePathValue.BITS;
		}
		mask |= value.ordinal() + 1;
		this.carryDirection.put(pos, mask);
	}

	private void checkBlock(long pos, int mask, int distance, int flags) {
		visited.add(pos);
		cursor1.set(pos);
		manager.taskSet.remove(cursor1);
		if (DEBUG) {
			BlockPhysicsDebug.debug(cursor1, Blocks.GLASS.defaultBlockState());
		}
		BlockState state = world.getBlockState(cursor1);
		BlockPhysicsData physicsData = getPhysics(cursor1, state);
		if (!physicsData.isNormal()) return;
		if ((mask & DIR_DOWN_MASK) != 0) {
			mask = removeDirection(mask, Direction.DOWN);
			if (tryCarry(pos, state, physicsData, Direction.DOWN, mask, distance / 2, flags & ~LONG_BEAM_FLAG)) {
				return;
			}
		}
		if ((mask & DIR_HORIZONTAL_MASK) != 0 && physicsData.haveLateralStrength()) {
			if (physicsData.beam() < physicsData.arc()) {
				flags |= ARC_FLAG;
				mask |= DIR_UP_MASK;
				requireSupport(pos, DIR_X_AXIS_MASK, DIR_Z_AXIS_MASK);
				cursor2.setWithOffset(cursor1, 0, 1, 0);
				requireTopSupport(cursor2.asLong());
			}
			for (Direction dir : DIR_HORIZONTAL) {
				int newMask;
				if ((newMask = removeDirection(mask, dir)) == mask) continue;
				mask = newMask;
				if (tryCarry(pos, state, physicsData, dir, mask, distance, flags)) {
					return;
				}
			}
		}
		if ((mask & DIR_UP_MASK) != 0) {
			if ((flags & ARC_FLAG) != 0) {
				distance = 0;
			}
			//mask = removeDirection(mask, Direction.UP);
			tryCarry(pos, state, physicsData, Direction.UP, 0, distance, flags & ~LONG_BEAM_FLAG);
		}
	}

	void unwind(long pos) {
		unwindQueue.add(pos);
		do {
			long lp = unwindQueue.popLong();
			unwindCursor1.set(lp);
			for (Direction dir : DIRECTIONS) {
				unwindCursor2.setWithOffset(unwindCursor1, dir);
				long pl = unwindCursor2.asLong();
				int mask = carryDirection.get(pl);
				while (mask != 0) {
					StablePathValue value = StablePathValue.byMask(mask);
					mask >>>= StablePathValue.BITS;
					Direction opposite = dir.getOpposite();
					if (opposite == value.direction) {
						if (!checkRequiredSupport(pl, opposite)) {
							continue;
						}
						nodeSet.remove(pl);
						if (pl == startPosL) {
							success = true;
							return;
						}
						unwindQueue.add(pl);
					}
				}
			}
		} while (!unwindQueue.isEmpty());
	}

	private boolean tryCarry(long p1l,
	                         BlockState state,
	                         BlockPhysicsData physicsData,
	                         Direction direction,
	                         int mask,
	                         int distance,
	                         int flags
	) {
		cursor2.setWithOffset(cursor1, direction);
		long p2l = cursor2.asLong();
		if (visited.contains(p2l)) return false;
		BlockState state2 = world.getBlockState(cursor2);
		BlockPhysicsData physicsData2 = getPhysics(cursor2, state2);
		if (canCarry(state, cursor1, physicsData, state2, cursor2, physicsData2, direction, distance, flags)) {
			stablePathCandidate(p1l, direction);
			if (physicsData2.isImmovable() || physicsData2.vanillaPhysics()) {
				unwind(p2l);
				return true;
			}
			if (mask != 0) {
				pushNode(p1l, mask, distance, flags);
			}
			if (direction.getAxis().isHorizontal()) {
				distance++;
			}
			pushNode(p2l, removeDirection(physicsData2.getDirMask(), direction.getOpposite()), distance, flags);
			return true;
		}
		return false;
	}

	private boolean canCarry(BlockState state,
	                         BlockPos pos,
	                         BlockPhysicsData physicsData,
	                         BlockState carryState,
	                         BlockPos carryPos,
	                         BlockPhysicsData carryPhysicsData,
	                         Direction direction,
	                         int distance,
	                         int flags
	) {
		if (carryPhysicsData.isAir()) {
			return false;
		}
		if (direction.getAxis().isHorizontal()) {
			if (!physicsData.haveLateralStrength() || !carryPhysicsData.haveLateralStrength()) {
				return false;
			}
			if ((flags & LONG_BEAM_FLAG) == 0 || !physicsData.longBeam() || !carryPhysicsData.longBeam()) {
				if (physicsData.lateralLimit() <= distance || carryPhysicsData.lateralLimit() <= distance) {
					return false;
				}
			}
		} else {
			if (direction == Direction.UP) {
				if ((flags & ARC_FLAG) == 0 && (!physicsData.hang() || !carryPhysicsData.hang())) {
					return false;
				}
			}
		}
		//if (carryPhysicsData.durability() < 0.1) return false;
		return shapesTouch(state, pos, carryState, carryPos, direction);
	}

	private boolean shapesTouch(BlockState state,
	                            BlockPos pos,
	                            BlockState carryState,
	                            BlockPos carryPos,
	                            Direction dir
	) {
		VoxelShape shape = state.getShape(world, pos);
		if (shape.isEmpty()) return false;
		VoxelShape shape2 = carryState.getShape(world, carryPos);
		if (shape2.isEmpty()) return false;
		VoxelShape block = Shapes.block();
		if (shape == block && shape2 == block) {
			return true;
		}
		return Shapes.joinIsNotEmpty(
				shape,
				shape2.move(
						dir.getStepX() * (1 - Shapes.BIG_EPSILON),
						dir.getStepY() * (1 - Shapes.BIG_EPSILON),
						dir.getStepZ() * (1 - Shapes.BIG_EPSILON)
				),
				BooleanOp.AND
		);
	}

	private BlockPhysicsData getPhysics(BlockPos pos, BlockState state) {
		return BlockPhysicsUtils.getPhysics(world, pos, state);
	}
}
