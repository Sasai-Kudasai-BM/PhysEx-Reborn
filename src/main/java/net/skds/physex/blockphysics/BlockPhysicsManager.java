package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.skds.physex.PhysExUtils;

import java.util.ArrayDeque;

public class BlockPhysicsManager {

	final ServerLevel world;

	private final ArrayDeque<BlockPhysicsChain> taskQueue = new ArrayDeque<>();
	private final ReferenceOpenHashSet<BlockPhysicsChain> taskQueueSet = new ReferenceOpenHashSet<>(16, .5f);
	final ObjectOpenHashSet<BlockPos> taskSet = new ObjectOpenHashSet<>(256, .5f);

	public BlockPhysicsManager(ServerLevel world) {
		this.world = world;
	}

	public static BlockPhysicsManager get(ServerLevel world) {
		return ((BlockPhysicsManagerGetter) world).physEx$getBlockPhysicsManager();
	}

	public void scheduleBlockCheck(BlockPhysicsChain chain) {
		if (taskQueueSet.add(chain)) {
			this.taskQueue.addLast(chain);
			this.taskSet.addAll(chain.leaves);
		}
	}

	public void scheduleBlockCheck(BlockPos pos) {
		BlockState bs = world.getBlockState(pos);
		BlockPhysicsData physics = BlockPhysicsUtils.getPhysics(world, pos, bs);
		if (physics.isNormal() && taskSet.add(pos)) {
			BlockPhysicsChain chain = new BlockPhysicsChain(this, pos, bs, physics);
			this.taskQueue.addLast(chain);
			this.taskQueueSet.add(chain);
		}
	}

	public void blockUpdated(BlockPos pos) {
		for (Direction dir : PhysExUtils.randomAllUpFirst()) {
			scheduleBlockCheck(pos.relative(dir));
		}
	}

	public void tick() {
		ProfilerFiller profiler = Profiler.get();
		profiler.push("block physics check");
		BlockPhysicsChain chain;
		while ((chain = this.taskQueue.pollFirst()) != null) {
			if (taskQueueSet.remove(chain)) {
				chain.tick();
			}
		}
		profiler.pop();
	}

}
