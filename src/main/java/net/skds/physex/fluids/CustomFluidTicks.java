package net.skds.physex.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import net.skds.physex.PhysExGameRules;
import net.skds.physex.fluids.layer.FluidLayer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;

public class CustomFluidTicks extends LevelTicks<Fluid> {

	private final LinkedList<EqualizationFluidTask> equalizationQueue = new LinkedList<>();
	private final HashSet<BlockPos> equalizationSet = new HashSet<>();
	private final HashSet<BlockPos> equalizationBlacklistSet = new HashSet<>();
	private final ServerLevel world;
	private final HashSet<ChunkAccess> updatedFluidOverrides = new HashSet<>();

	private long taskCounter;
	private long blockReadCounter;
	private long blockUpdateCounter;

	public CustomFluidTicks(LongPredicate chunkPredicate, ServerLevel world) {
		super(chunkPredicate);
		this.world = world;
	}

	public static CustomFluidTicks get(LevelAccessor world) {
		return (CustomFluidTicks) world.getFluidTicks();
	}

	public int getTaskLimit() {
		return world.getGameRules().getInt(PhysExGameRules.TASK_LIMIT);
	}

	public void fluidLayerUpdate(ChunkAccess chunk, BlockPos pos) {
		updatedFluidOverrides.add(chunk);
	}

	public void countTask() {
		taskCounter++;
	}

	public void countBlockRead() {
		blockReadCounter++;
	}

	public boolean allowedForEqualization(BlockPos pos) {
		return !equalizationBlacklistSet.contains(pos);
	}

	public void countBlockUpdate(BlockPos pos) {
		blockUpdateCounter++;
		equalizationBlacklistSet.add(pos);
	}

	public void done(BlockPos pos) {
		equalizationSet.remove(pos);
	}

	public void scheduleEqualization(BlockPos pos, FlowingFluid fluid, ServerLevel world) {
		if (equalizationSet.add(pos)) {
			equalizationQueue.add(new EqualizationFluidTask(pos, fluid, world));
		}
	}

	@Override
	public void tick(long time, int limit, BiConsumer<BlockPos, Fluid> biConsumer) {
		taskCounter = 0;
		blockReadCounter = 0;
		blockUpdateCounter = 0;
		super.tick(time, getTaskLimit(), biConsumer);
		ProfilerFiller profiler = Profiler.get();

		int eqLim = getTaskLimit() / 8;
		long equalizationTaskCounter = 0;
		//if (taskCounter > 0)
		//	System.out.printf("t:%s \te:%s \tr:%s \tu:%s\n", taskCounter, equalizationSet.size(), blockReadCounter, blockUpdateCounter);

		profiler.push("fluid equalization");
		EqualizationFluidTask task;
		while (equalizationTaskCounter < eqLim && (task = equalizationQueue.pollFirst()) != null) {
			if (equalizationSet.remove(task.pos) && !equalizationBlacklistSet.contains(task.pos)) {
				task.run();
				equalizationTaskCounter++;
			}
		}
		if (FluidUtils.FLUID_CHUNK_LAYER && !updatedFluidOverrides.isEmpty()) {
			profiler.popPush("updating fluid chunks");
			for (ChunkAccess ca : updatedFluidOverrides) {
				FluidLayer.update(ca);
			}
			updatedFluidOverrides.clear();
		}
		profiler.pop();
		//equalizationSet.clear();
		//equalizationQueue.clear();
		equalizationBlacklistSet.clear();
	}
}
