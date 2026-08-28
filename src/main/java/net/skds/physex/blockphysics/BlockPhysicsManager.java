package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayDeque;

public class BlockPhysicsManager {

	final ServerLevel world;
	final LongOpenHashSet taskSet = new LongOpenHashSet(256, .5f);
	final LongOpenHashSet fallBlockSet = new LongOpenHashSet(256, .5f);
	private final ArrayDeque<BlockFallInfo> fallBlocks = new ArrayDeque<>(256);
	private final LongArrayFIFOQueue taskQueue = new LongArrayFIFOQueue(256);
	private final BlockPhysicsChain chain;
	private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

	public BlockPhysicsManager(ServerLevel world) {
		this.world = world;
		this.chain = new BlockPhysicsChain(this);
	}

	public static BlockPhysicsManager get(ServerLevel world) {
		return ((BlockPhysicsManagerGetter) world).physEx$getBlockPhysicsManager();
	}

	public void scheduleBlockCheck(BlockPos pos) {
		if (!world.shouldTickBlocksAt(pos)) return;
		long lp = pos.asLong();
		if (taskSet.add(lp)) {
			taskQueue.enqueue(lp);
		}
	}

	public void blockUpdated(BlockPos pos) {
		scheduleBlockCheck(pos);
		for (Direction dir : BlockPhysicsUtils.DIRECTIONS) {
			scheduleBlockCheck(pos.relative(dir));
		}
	}

	public void tick() {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("block physics check");
		if (chain.getState() == PhysicsChainState.WORKING_NEXT_TICK) {
			chain.nextTick();
		} else while (!taskQueue.isEmpty()) {
			long pos = taskQueue.dequeueLong();
			if (!taskSet.remove(pos)) continue;

			if (!chain.init(pos)) continue;
			chain.tick();
			PhysicsChainState state = chain.getState();
			if (state == PhysicsChainState.WORKING_NEXT_TICK) break;
			if (state == PhysicsChainState.DONE && chain.willFall()) {
				willFall(chain.getFall());
			}
		}
		profiler.popPush("block physics breaking blocks");
		fallBlocks();
		profiler.pop();
	}

	private void willFall(BlockFallInfo bfi) {
		cursor.set(bfi.pos());
		world.setBlock(cursor, BlockPhysicsUtils.AIR, 3);
		//if (fallBlockSet.add(bfi.pos())) {
		//	fallBlocks.addLast(bfi);
		//}
	}

	private void fallBlocks() {
		BlockFallInfo bfi;
		while ((bfi = fallBlocks.pollFirst()) != null) {
			long p = bfi.pos();
			if (!fallBlockSet.remove(p)) continue;
			cursor.set(p);
			world.setBlock(cursor, BlockPhysicsUtils.AIR, 3);
		}
	}
}
