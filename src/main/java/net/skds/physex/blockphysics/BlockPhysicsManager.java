package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.skds.physex.blockphysics.path.StablePath;
import net.skds.physex.blockphysics.path.StablePathValue;

public class BlockPhysicsManager {

	final ServerLevel world;
	final LongOpenHashSet taskSet = new LongOpenHashSet(256, .5f);
	private final LongArrayFIFOQueue taskQueue = new LongArrayFIFOQueue(256);
	private final BlockPhysicsChain chain;
	private final BlockPos.MutableBlockPos cursor1 = new BlockPos.MutableBlockPos();
	private final LongArrayList stablePopQueue = new LongArrayList(256);

	private long lastStablePathPos = -1;
	private StablePath lastStablePath;

	public BlockPhysicsManager(ServerLevel world) {
		this.world = world;
		this.chain = new BlockPhysicsChain(this);
	}

	public static BlockPhysicsManager get(ServerLevel world) {
		return ((BlockPhysicsManagerGetter) world).physEx$getBlockPhysicsManager();
	}

	public void markStable(long pos, Direction dir, int distance) {
		long secP = SectionPos.blockToSection(pos);
		StablePath sp;
		cursor1.set(pos);
		if (secP == this.lastStablePathPos && this.lastStablePath != null) {
			sp = this.lastStablePath;
		} else {
			sp = BlockPhysicsUtils.getOrCreateStablePath(world, cursor1);
			this.lastStablePath = sp;
			this.lastStablePathPos = secP;
		}
		if (sp.set(cursor1.getX(), cursor1.getY(), cursor1.getZ(), StablePathValue.byDirection(dir), distance)) {
			BlockPhysicsUtils.markSectionDirty(world, cursor1);
		}
	}

	public StablePath getStablePath(BlockPos pos) {
		long secP = SectionPos.asLong(pos);
		StablePath sp;
		if (secP == this.lastStablePathPos) {
			sp = this.lastStablePath;
		} else {
			sp = BlockPhysicsUtils.getStablePath(world, pos);
			this.lastStablePath = sp;
			this.lastStablePathPos = secP;
		}
		return sp;
	}

	public void popStable(BlockPos pos) {
		StablePath sp = getStablePath(pos);
		if (sp == null) return;
		sp.remove(pos.getX(), pos.getY(), pos.getZ());
		stablePopQueue.add(pos.asLong());
		do {
			long p = stablePopQueue.popLong();
			for (Direction dir : BlockPhysicsUtils.DIRECTIONS) {
				cursor1.set(p).move(dir);
				scheduleBlockCheck(cursor1);
				sp = getStablePath(cursor1);
				if (sp == null) continue;
				StablePathValue spv = sp.get(cursor1.getX(), cursor1.getY(), cursor1.getZ());
				if (spv.direction == dir.getOpposite()) {
					sp.remove(cursor1.getX(), cursor1.getY(), cursor1.getZ());
					BlockPhysicsUtils.markSectionDirty(world, cursor1);
					stablePopQueue.add(cursor1.asLong());
				}
			}
		} while (!stablePopQueue.isEmpty());
	}

	public void scheduleBlockCheck(BlockPos pos) {
		if (!world.shouldTickBlocksAt(pos)) return;
		long lp = pos.asLong();
		if (taskSet.add(lp)) {
			taskQueue.enqueue(lp);
		}
	}

	public void blockUpdated(BlockPos pos) {
		popStable(pos);
		scheduleBlockCheck(pos);
		for (Direction dir : BlockPhysicsUtils.DIRECTIONS) {
			scheduleBlockCheck(cursor1.setWithOffset(pos, dir));
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
		profiler.pop();
	}

	private void willFall(BlockFallInfo bfi) {
		cursor1.set(bfi.pos());
		world.setBlock(cursor1.immutable(), BlockPhysicsUtils.AIR, 3);
	}
}
