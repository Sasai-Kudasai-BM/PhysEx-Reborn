package net.skds.physex.blockphysics.path;

import net.minecraft.core.SectionPos;
import net.skds.physex.blockphysics.BlockPhysicsManager;
import net.skds.physex.utils.PalettedData;

import static net.skds.physex.blockphysics.BlockPhysicsUtils.sectionIndex;

public final class StablePath {

	private SectionPos pos;
	private final PalettedData data;
	private final BlockPhysicsManager manager;

	public StablePath(BlockPhysicsManager manager) {
		this.manager = manager;
		this.data = new PalettedData(StablePathValue.BITS, 4096);
	}

	public StablePathValue get(int x, int y, int z) {
		int value = data.getValue(sectionIndex(x, y, z));
		return StablePathValue.byId(value);
	}

	public void set(int x, int y, int z, StablePathValue direction) {
		int value = direction.ordinal();
		data.setValue(sectionIndex(x, y, z), value);
	}


}
