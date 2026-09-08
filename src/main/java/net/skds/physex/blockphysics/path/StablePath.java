package net.skds.physex.blockphysics.path;

import net.minecraft.core.SectionPos;

import static net.skds.physex.blockphysics.BlockPhysicsUtils.sectionIndex;

public final class StablePath {

	private SectionPos pos;
	private final byte[] data;

	public StablePath(byte[] data) {
		this.data = data;
	}

	public StablePathValue get(int x, int y, int z) {
		int value = data[sectionIndex(x, y, z)];
		return StablePathValue.byMask(value);
	}

	public int getDistance(int x, int y, int z) {
		byte value = data[sectionIndex(x, y, z)];
		return value >>> StablePathValue.BITS;
	}

	public void set(int x, int y, int z, StablePathValue direction, int distance) {
		int value = (direction.ordinal() | (distance << StablePathValue.BITS)) & 0xFF;
		data[sectionIndex(x, y, z)] = (byte) value;
	}

	public void remove(int x, int y, int z, StablePathValue direction, int distance) {
		int value = (direction.ordinal() | (distance << StablePathValue.BITS)) & 0xFF;
		data[sectionIndex(x, y, z)] = (byte) value;
	}
}
