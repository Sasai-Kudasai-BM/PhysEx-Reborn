package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skds.physex.PhysEx;

import java.util.Arrays;

import static net.skds.physex.blockphysics.BlockPhysicsUtils.*;

public final class BlockPhysicsChain {

	private static final boolean DEBUG = PhysEx.DEBUG;
	private static final int INITIAL_SIZE = 256;

	private final BlockPos.MutableBlockPos cursor1 = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos cursor2 = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos unwindCursor1 = new BlockPos.MutableBlockPos();
	private final BlockPos.MutableBlockPos unwindCursor2 = new BlockPos.MutableBlockPos();

	private final LongOpenHashSet visited = new LongOpenHashSet(INITIAL_SIZE, .5f);
	private final Long2ByteOpenHashMap carryDirection = new Long2ByteOpenHashMap(INITIAL_SIZE, .5f);
	private final LongOpenHashSet unwindVisited = new LongOpenHashSet(INITIAL_SIZE, .5f);
	private final LongArrayList unwindQueue = new LongArrayList(INITIAL_SIZE);
	private final Long2ObjectOpenHashMap<BlockState> blockStateCache = new Long2ObjectOpenHashMap<>(INITIAL_SIZE, .5f);

	private final LongOpenHashSet nodeSet = new LongOpenHashSet(INITIAL_SIZE, .5f);
	private Stack stack = new Stack();
	private Stack stackUp = new Stack();

	private final BlockPhysicsManager manager;
	private final ServerLevel world;

	private long startPos;
	private boolean hold = false;
	@Getter
	private PhysicsChainState state = PhysicsChainState.WAIT;
	@Getter
	private BlockFallInfo fall = null;

	public BlockPhysicsChain(BlockPhysicsManager manager) {
		this.manager = manager;
		this.world = manager.world;
	}

	public boolean willFall() {
		return !hold;
	}

	private void done(boolean success) {
		this.fall = new BlockFallInfo(startPos, Direction.DOWN);
		this.hold = success;
		this.state = PhysicsChainState.DONE;
	}

	private void pushNode(long pos, int dirMask, int distance, int flags) {
		if (dirMask == 0 || !nodeSet.add(pos)) return;
		Stack stk = this.stack;
		if (stk.isEmpty()) {
			stk.pushNode(pos, dirMask, distance, flags);
			return;
		}
		long headPos = stk.nodes[stk.head];
		int headY = BlockPos.getY(headPos);
		int targetY = BlockPos.getY(pos);
		if (targetY > headY) {
			stk = this.stackUp;
		}
		stk.pushNode(pos, dirMask, distance, flags);
	}

	public boolean init(long startPos) {
		clear();
		cursor1.set(startPos);
		BlockState state = getBlockState(cursor1, startPos);
		BlockPhysicsData physicsData = getPhysics(cursor1, state);
		if (!physicsData.isNormal()) {
			return false;
		}
		while (true) {
			manager.taskSet.remove(startPos);
			cursor2.setWithOffset(cursor1, 0, -1, 0);
			BlockState state2 = getBlockState(cursor2);
			BlockPhysicsData physicsData2 = getPhysics(cursor2, state2);
			if (canCarry(state, cursor1, physicsData, state2, cursor2, physicsData2, Direction.DOWN, 0, 0)) {
				if (physicsData2.isImmovable() || physicsData2.vanillaPhysics()) {
					if (DEBUG) {
						BlockPhysicsDebug.debug(cursor1, Blocks.BEDROCK.defaultBlockState());
					}
					return false;
				}
				cursor1.set(cursor2);
				state = state2;
				physicsData = physicsData2;
			} else {
				startPos = cursor1.asLong();
				break;
			}
		}

		this.startPos = startPos;
		pushNode(this.startPos, physicsData.getDirMask(), 0, 0);
		return true;
	}

	private Stack nextStack() {
		Stack current = this.stack;
		if (current.isEmpty()) {
			Stack up = this.stackUp;
			if (up.isEmpty()) {
				return null;
			}
			this.stack = up;
			this.stackUp = current;
			current = up;
		}
		return current;
	}

	public void nextTick() {
		blockStateCache.clear();
		tick();
	}

	public void clear() {
		this.visited.clear();
		this.carryDirection.clear();
		this.unwindVisited.clear();
		this.unwindQueue.clear();
		this.blockStateCache.clear();
		this.nodeSet.clear();
		this.stack.clear();
		this.stackUp.clear();
		this.hold = false;
		this.fall = null;
		this.state = PhysicsChainState.WAIT;
	}

	public void tick() {
		if (state == PhysicsChainState.WORKING_NEXT_TICK) state = PhysicsChainState.WORKING;
		long pos;
		int dist;
		int mask;
		int flags;
		int i = 15_000;
		Stack stk;
		while ((stk = nextStack()) != null) {
			if (--i <= 0) {
				if (DEBUG) {
					BlockPhysicsDebug.debug(startPos, Blocks.REDSTONE_BLOCK.defaultBlockState());
				}
				state = PhysicsChainState.WORKING_NEXT_TICK;
				return;
			}
			if (hold) {
				break;
			}
			int head = stk.head;
			pos = stk.nodes[head];
			dist = stk.nodeDistance[head];
			flags = stk.nodeFlags[head];
			mask = stk.nodeDirections[head];
			stk.head--;
			if (!nodeSet.remove(pos)) {
				continue;
			}
			checkBlock(pos, mask, dist, flags);
		}
		if (state != PhysicsChainState.DONE) {
			done(false);
		}
		if (DEBUG) {
			if (hold) {
				BlockPhysicsDebug.debug(startPos, Blocks.BLUE_STAINED_GLASS.defaultBlockState());
			} else {
				BlockPhysicsDebug.debug(startPos, Blocks.RED_STAINED_GLASS.defaultBlockState());
				//world.destroyBlock(startPos, false);
			}
		}
	}

	private void stablePathCandidate(long pos, Direction value) {
		int mask = carryDirection.get(pos);
		mask |= 1 << value.ordinal();
		this.carryDirection.put(pos, (byte) mask);
	}

	private void checkBlock(long pos, int mask, int distance, int flags) {
		visited.add(pos);
		cursor1.set(pos);
		//manager.taskSet.remove(pos);
		//if (DEBUG) {
		//	BlockPhysicsDebug.debug(cursor1, Blocks.GLASS.defaultBlockState());
		//}
		BlockState state = getBlockState(cursor1);
		BlockPhysicsData physicsData = getPhysics(cursor1, state);
		if (!physicsData.isNormal()) return;
		if ((mask & DIR_DOWN_MASK) != 0) {
			mask = removeDirection(mask, Direction.DOWN);
			if (tryCarry(pos, state, physicsData, Direction.DOWN, mask, distance, flags)) {
				return;
			}
		}
		if ((mask & DIR_HORIZONTAL_MASK) != 0) {
			for (Direction dir : DIR_HORIZONTAL) {
				int newMask;
				if ((newMask = removeDirection(mask, dir)) == mask) continue;
				mask = newMask;
				if (tryCarry(pos, state, physicsData, dir, mask, distance, flags)) {
					return;
				}
			}
		}
		if ((mask & DIR_UP_MASK) != 0 && distance < physicsData.beam()) {
			if (tryCarry(pos, state, physicsData, Direction.UP, 0, distance, flags)) {
				return;
			}
		}
		visited.remove(pos);
	}

	void unwind(long pos) {
		unwindQueue.add(pos);
		do {
			unwindCursor1.set(unwindQueue.popLong());
			br:
			for (Direction dir : DIRECTIONS) {
				unwindCursor2.setWithOffset(unwindCursor1, dir);
				long pl = unwindCursor2.asLong();
				int mask = carryDirection.get(pl);
				Direction opposite = dir.getOpposite();
				for (int i = 0; i < DIRECTIONS.length && mask != 0; i++) {
					if ((mask & 1) != 0 && opposite == DIRECTIONS[i]) {
						//if (DEBUG) {
						//	BlockPhysicsDebug.debug(unwindCursor2, Blocks.LIME_STAINED_GLASS.defaultBlockState());
						//}
						if (pl == startPos) {
							done(true);
							return;
						}
						if (!unwindVisited.add(pl)) continue;
						nodeSet.remove(pl);
						unwindQueue.add(pl);
						if (opposite == Direction.DOWN) {
							//if (DEBUG) {
							//	BlockPhysicsDebug.debug(unwindCursor1, Blocks.LAPIS_BLOCK.defaultBlockState());
							//}
							break br;
						}
					}
					mask >>>= 1;
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
		BlockState state2 = getBlockState(cursor2);
		BlockPhysicsData physicsData2 = getPhysics(cursor2, state2);
		int nextDist = distance;
		if (direction.getAxis().isHorizontal() && physicsData2.beam() >= physicsData.beam()) {
			nextDist++;
		}
		if (canCarry(state, cursor1, physicsData, state2, cursor2, physicsData2, direction, nextDist, flags)) {
			stablePathCandidate(p1l, direction);
			if (physicsData2.isImmovable() || physicsData2.vanillaPhysics()) {
				unwind(p2l);
				return true;
			}
			if (direction != Direction.DOWN) {
				pushNode(p2l, removeDirection(physicsData2.getDirMask(), direction.getOpposite()), nextDist, flags);
			}
			if (mask != 0) {
				pushNode(p1l, mask, distance, flags);
			}
			if (direction == Direction.DOWN) {
				pushNode(p2l, removeDirection(physicsData2.getDirMask(), direction.getOpposite()), 0, flags);
			}
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
			if (physicsData.beam() < distance || carryPhysicsData.beam() < distance) {
				return false;
			}
		} else {
			if (direction == Direction.UP) {
				if (!physicsData.tensile() || !carryPhysicsData.tensile()) {
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
		VoxelShape shape = state.getCollisionShape(world, pos);
		if (shape.isEmpty()) return false;
		VoxelShape shape2 = carryState.getCollisionShape(world, carryPos);
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

	private BlockState getBlockState(BlockPos pos) {
		long lp = pos.asLong();
		if (manager.fallBlockSet.contains(lp)) return AIR;
		BlockState state = blockStateCache.get(lp);
		if (state == null) {
			state = world.getBlockState(pos);
			blockStateCache.put(lp, state);
		}
		return state;
	}

	private BlockState getBlockState(BlockPos pos, long lp) {
		if (manager.fallBlockSet.contains(lp)) return AIR;
		BlockState state = blockStateCache.get(lp);
		if (state == null) {
			state = world.getBlockState(pos);
			blockStateCache.put(lp, state);
		}
		return state;
	}

	private BlockPhysicsData getPhysics(BlockPos pos, BlockState state) {
		return BlockPhysicsUtils.getPhysics(world, pos, state);
	}

	private static class Stack {

		private long[] nodes = new long[8];
		private byte[] nodeDirections = new byte[nodes.length];
		private byte[] nodeFlags = new byte[nodes.length];
		private short[] nodeDistance = new short[nodes.length];
		private int head = -1;

		private void pushNode(long pos, int dirMask, int distance, int flags) {
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

		private boolean isEmpty() {
			return head < 0;
		}

		private void clear() {
			this.head = -1;
		}
	}
}
