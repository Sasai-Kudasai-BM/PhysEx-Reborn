package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.skds.physex.PhysEx;

import java.util.ArrayDeque;

public final class BlockPhysicsChain {

	private static final boolean DEBUG = PhysEx.DEBUG;

	private final ObjectOpenHashSet<BlockPos> unsupported = new ObjectOpenHashSet<>(16, .5f);
	private final ObjectOpenHashSet<BlockPos> leavesSet = new ObjectOpenHashSet<>(16, .5f);
	final ArrayDeque<BlockPos> leaves = new ArrayDeque<>();
	private final BlockPhysicsManager manager;
	private final ServerLevel world;

	public BlockPhysicsChain(BlockPhysicsManager manager, BlockPos startPos, BlockState startState, BlockPhysicsData physics) {
		this.manager = manager;
		this.world = manager.world;
		addLeaf(startPos);
	}

	private void addLeaf(BlockPos pos) {
		if (this.leavesSet.add(pos)) {
			this.leaves.addLast(pos);
		}
	}

	public void tick() {
		BlockPos leaf;
		while ((leaf = leaves.pollFirst()) != null) {
			if (!leavesSet.remove(leaf)) continue;
			manager.taskSet.remove(leaf);
			if (DEBUG) {
				BlockPhysicsDebug.debug(leaf, Blocks.GLASS.defaultBlockState());
			}
		}
	}
}
