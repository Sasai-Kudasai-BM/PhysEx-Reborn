package net.skds.physex.fluids;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;

public record FluidDistribution(Object2IntOpenHashMap<BlockPos> occupied, int remainingFluid) {

	public static final FluidDistribution EMPTY = new FluidDistribution(null, 0);

	public static FluidDistribution single(BlockPos pos, int amount, int remaining) {
		Object2IntOpenHashMap<BlockPos> map = new Object2IntOpenHashMap<>(1);
		map.put(pos, amount);
		return new FluidDistribution(map, remaining);
	}

	public boolean isEmpty() {
		return remainingFluid == 0 && occupied.isEmpty();
	}
}
