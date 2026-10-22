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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skds.lib2.shapes.FastAABBTree;
import net.skds.physex.blockphysics.entity.BPFallingBlock;
import net.skds.physex.blockphysics.path.StablePath;
import net.skds.physex.blockphysics.path.StablePathValue;

import java.util.ArrayList;

public class BlockPhysicsManager {

	final ServerLevel world;
	final LongOpenHashSet taskSet = new LongOpenHashSet(256, .5f);
	private final LongArrayFIFOQueue taskQueue = new LongArrayFIFOQueue(256);
	private final BlockPhysicsChain chain;
	private final BlockPos.MutableBlockPos cursor1 = new BlockPos.MutableBlockPos();
	private final LongArrayList stablePopQueue = new LongArrayList(256);

	private final FastAABBTree<BPFallingBlock> fallingBlockTree = new FastAABBTree<>(64);
	private final ArrayList<BPFallingBlock> fallingBlockTickQueue = new ArrayList<>(64);

	private long lastStablePathPos = -1;
	private StablePath lastStablePath;

	public BlockPhysicsManager(ServerLevel world) {
		this.world = world;
		this.chain = new BlockPhysicsChain(this);
	}

	public static BlockPhysicsManager get(ServerLevel world) {
		return ((BlockPhysicsManagerGetter) world).physEx$getBlockPhysicsManager();
	}

	public int addFallingBlock(BPFallingBlock fallingBlock) {
		AABB aabb = fallingBlock.getBroadAABB();
		return fallingBlockTree.put(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, fallingBlock);
	}

	public void updateFallingBlock(BPFallingBlock fallingBlock) {
		AABB aabb = fallingBlock.getBroadAABB();
		fallingBlockTree.move(fallingBlock.getTreeNode(), aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}

	public void removeFallingBlock(int index) {
		fallingBlockTree.remove(index);
	}

	public void markStable(long pos, Direction dir, int distance) {
		long secP = SectionPos.blockToSection(pos);
		cursor1.set(pos);
		taskSet.remove(pos);
		StablePath sp;
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
		scheduleBlockCheck(pos);
		for (Direction dir : BlockPhysicsUtils.DIRECTIONS) {
			scheduleBlockCheck(cursor1.setWithOffset(pos, dir));
		}
	}

	public void tickAfterEntities() {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("falling block tick");

		fallingBlockTree.forEach((b, i) -> {
			fallingBlockTickQueue.add(b);
			Vec3 v1 = b.getRelV();
			AABB broadBox = b.getBroadAABB();
			AABB box1 = b.getBoundingBox();
			fallingBlockTree.forEachCollision(broadBox.minX, broadBox.minY, broadBox.minZ, broadBox.maxX, broadBox.maxY, broadBox.maxZ, i,
					(b2, i2) -> {
						Vec3 v2 = b2.getRelV();
						var cr = BlockPhysicsUtils.sweptAABB(b2.getBoundingBox(), box1, v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
						if (cr != null) {
							b.setCollision(cr, b2);
						}
					}
			);
		});
		fallingBlockTickQueue.forEach(BPFallingBlock::postCollisionTick1);
		fallingBlockTickQueue.forEach(BPFallingBlock::postCollisionTick2);
		fallingBlockTickQueue.clear();
		profiler.pop();
	}

	public void tickBlocks() {
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
		world.setBlock(bfi.pos(), BlockPhysicsUtils.AIR, 3);
		BPFallingBlock fallingBlock = new BPFallingBlock(world, bfi.state(), bfi.pos(), bfi.physicsData());
		fallingBlock.setDeltaMovement(0, fallingBlock.getGravity() * -2, 0);
		world.addFreshEntity(fallingBlock);
	}
}
