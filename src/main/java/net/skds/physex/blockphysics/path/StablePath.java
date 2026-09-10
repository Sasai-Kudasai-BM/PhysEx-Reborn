package net.skds.physex.blockphysics.path;

import net.minecraft.core.BlockPos;

import static net.skds.physex.blockphysics.BlockPhysicsUtils.sectionIndex;

public final class StablePath {

	private final byte[] data;

	public StablePath(byte[] data) {
		this.data = data;
	}

	public StablePathValue get(BlockPos pos) {
		int value = data[sectionIndex(pos.getX(), pos.getY(), pos.getZ())];
		return StablePathValue.byMask(value);
	}

	public StablePathValue get(int x, int y, int z) {
		int value = data[sectionIndex(x, y, z)];
		return StablePathValue.byMask(value);
	}

	public int getDistance(BlockPos pos) {
		byte value = data[sectionIndex(pos.getX(), pos.getY(), pos.getZ())];
		return value >>> StablePathValue.BITS;
	}

	public int getDistance(int x, int y, int z) {
		byte value = data[sectionIndex(x, y, z)];
		return value >>> StablePathValue.BITS;
	}

	public boolean set(int x, int y, int z, StablePathValue direction, int distance) {
		byte value = (byte) ((direction.ordinal() | (distance << StablePathValue.BITS)) & 0xFF);
		int index = sectionIndex(x, y, z);
		if (data[index] != value) {
			data[index] = value;
			return true;
		}
		return false;
	}

	public void remove(int x, int y, int z) {
		data[sectionIndex(x, y, z)] = (byte) 0;
	}
}
