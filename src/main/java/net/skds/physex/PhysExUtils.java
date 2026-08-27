package net.skds.physex;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.skds.lib2.utils.ArrayUtils;

@UtilityClass
public class PhysExUtils {

	public static final int NATURAL_BLOCK_FLAG = Block.UPDATE_KNOWN_SHAPE;

	private static final Direction[][] SHUFFLE_H = {
			{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
			{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH},
			{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST},
			{Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH},
			{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH},
			{Direction.EAST, Direction.SOUTH, Direction.NORTH, Direction.WEST},
			{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
			{Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH},
			{Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.EAST},
			{Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
			{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH},
			{Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.EAST},
			{Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH},
			{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
			{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST},
			{Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH},
			{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST},
			{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST},
			{Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH},
			{Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST},
			{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST},
			{Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH},
			{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST},
			{Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST},
	};
	private static final Direction[][] SHUFFLE_DOWN_FIRST;
	private static final Direction[][] SHUFFLE_UP_FIRST;

	static {
		Direction[][] shuffle = new Direction[SHUFFLE_H.length][];
		Direction[][] shuffleInv = new Direction[SHUFFLE_H.length][];
		for (int i = 0; i < shuffle.length; i++) {
			Direction[] layer = new Direction[6];
			System.arraycopy(SHUFFLE_H[i], 0, layer, 1, 4);
			layer[0] = Direction.DOWN;
			layer[5] = Direction.UP;
			shuffle[i] = layer;
			layer = layer.clone();
			layer[0] = Direction.UP;
			layer[5] = Direction.DOWN;
			shuffleInv[i] = layer;
		}
		SHUFFLE_DOWN_FIRST = shuffle;
		SHUFFLE_UP_FIRST = shuffleInv;
	}


	public static Direction[] randomHorizontal() {
		return ArrayUtils.getRandom(SHUFFLE_H);
	}

	public static Direction[] randomAllDownFirst() {
		return ArrayUtils.getRandom(SHUFFLE_DOWN_FIRST);
	}

	public static Direction[] randomAllUpFirst() {
		return ArrayUtils.getRandom(SHUFFLE_UP_FIRST);
	}

}
