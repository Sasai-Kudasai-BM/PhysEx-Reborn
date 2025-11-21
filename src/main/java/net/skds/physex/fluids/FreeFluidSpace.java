package net.skds.physex.fluids;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;

public record FreeFluidSpace(Object2IntOpenHashMap<BlockPos> positions, int remaining) {

	public static final FreeFluidSpace EMPTY = new FreeFluidSpace(null, 0);

	public static FreeFluidSpace single(BlockPos pos, int amount, int remaining) {
		Object2IntOpenHashMap<BlockPos> map = new Object2IntOpenHashMap<>(1);
		map.put(pos, amount);
		return new FreeFluidSpace(map, remaining);
	}

	public boolean isEmpty() {
		return remaining == 0 && positions.isEmpty();
	}
}
