package net.skds.physex.blockphysics.path;

import lombok.AllArgsConstructor;
import net.minecraft.core.Direction;

@AllArgsConstructor
public enum StablePathValue {
	EMPTY(null),
	DOWN(Direction.DOWN),
	UP(Direction.UP),
	NORTH(Direction.NORTH),
	SOUTH(Direction.SOUTH),
	WEST(Direction.WEST),
	EAST(Direction.EAST),
	SPECIAL(null);

	public final Direction direction;

	public static final int BITS = 3;
	public static final int MASK = ~(-1 << BITS);
	private static final StablePathValue[] values = values();

	public static StablePathValue byId(int id) {
		return values[id];
	}

	public static StablePathValue byMask(int mask) {
		return values[mask & MASK];
	}

	public static StablePathValue byDirection(Direction dir) {
		return values[dir.ordinal() + 1];
	}
}
