package net.skds.physex.fluids;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import net.skds.physex.fluids.layer.FluidLayer;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;

public class CustomFluidTicks extends LevelTicks<Fluid> {

	private final ArrayDeque<EqualizationFluidTask> equalizationQueue = new ArrayDeque<>();
	private final ObjectOpenHashSet<BlockPos> equalizationSet = new ObjectOpenHashSet<>(256, .5f);
	private final ServerLevel world;
	private final ObjectOpenHashSet<ChunkAccess> updatedFluidOverrides = new ObjectOpenHashSet<>(64, .5f);

	private final Long2IntOpenHashMap equalizationBlacklistSet = new Long2IntOpenHashMap(512, .5f);
	private final Int2ObjectOpenHashMap<LongOpenHashSet> equalizationBlacklistMap = new Int2ObjectOpenHashMap<>(16, .5f);

	private long taskCounter;
	private long blockReadCounter;
	private long blockUpdateCounter;

	private int tickNumber;

	public CustomFluidTicks(LongPredicate chunkPredicate, ServerLevel world) {
		super(chunkPredicate);
		this.world = world;
	}

	public static CustomFluidTicks get(LevelAccessor world) {
		return (CustomFluidTicks) world.getFluidTicks();
	}

	public int getTaskLimit() {
		return world.getGameRules().get(PhysExFluidGameRules.TASK_LIMIT);
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
		return !equalizationBlacklistSet.containsKey(pos.asLong());
	}

	public void onBlockUpdate(BlockPos pos, int tickDelay) {
		blockUpdateCounter++;
		tickDelay = FluidUtils.modifyTickRate(tickDelay);
		tickDelay--;
		if (tickDelay < 1) return;
		long p = pos.asLong();
		int t = tickNumber + tickDelay;
		int oldT = equalizationBlacklistSet.getOrDefault(p, -1);
		if (oldT < t) {
			equalizationBlacklistSet.put(p, t);
			var set = equalizationBlacklistMap.computeIfAbsent(t, ignored -> new LongOpenHashSet(256, .5f));
			set.add(p);
			if (oldT >= 0) {
				set = equalizationBlacklistMap.get(oldT);
				Objects.requireNonNull(set);
				set.remove(p);
			}
		}
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

		int eqLim = getTaskLimit() / 100;
		if (eqLim < 1) eqLim = 1;
		long equalizationTaskCounter = 0;
		//if (taskCounter > 0)
		//	System.out.printf("t:%s \te:%s \tr:%s \tu:%s\n", taskCounter, equalizationSet.size(), blockReadCounter, blockUpdateCounter);

		profiler.push("fluid equalization");
		EqualizationFluidTask task;
		EqualizationFluidTask bt = equalizationQueue.peekLast();
		while (equalizationTaskCounter < eqLim && (task = equalizationQueue.pollFirst()) != null) {
			if (bt == task) {
				bt = null;
			}
			if (equalizationSet.remove(task.pos)) {
				if (!equalizationBlacklistSet.containsKey(task.pos.asLong())) {
					task.run();
					equalizationTaskCounter++;
				}
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

		var set = equalizationBlacklistMap.remove(tickNumber);
		if (set != null) {
			LongIterator itr = set.iterator();
			while (itr.hasNext()) {
				equalizationBlacklistSet.remove(itr.nextLong());
			}
		}
		if (bt == null) {
			tickNumber++;
		}
	}
}
