package net.skds.physex.blockphysics.chunk;

import lombok.Getter;
import net.skds.physex.blockphysics.BlockPhysicsUtils;
import net.skds.physex.blockphysics.path.StablePath;

import java.util.BitSet;


@Getter
public class BPChunkSection {
	private final int index;
	private BitSet naturals;
	private byte[] stablePathData;
	private StablePath stablePath;

	public BPChunkSection(int index) {
		this.index = index;
	}

	public BPChunkSection(int index, long[] naturals, byte[] stablePathData) {
		this.index = index;
		if (naturals != null && naturals.length != 0) {
			this.naturals = BitSet.valueOf(naturals);
		}
		this.stablePathData = stablePathData;
	}

	public boolean isNatural(int x, int y, int z) {
		BitSet nat = this.naturals;
		if (nat == null) return true;
		return !nat.get(BlockPhysicsUtils.sectionIndex(x, y, z));
	}

	public void setNatural(int x, int y, int z, boolean value) {
		int index = BlockPhysicsUtils.sectionIndex(x, y, z);
		if (value) {
			BitSet nat = this.naturals;
			if (nat != null) nat.clear(index);
		} else {
			getOrInitNaturals().set(index);
		}
	}

	public BitSet getOrInitNaturals() {
		BitSet nat = this.naturals;
		if (nat == null) {
			nat = new BitSet(4096);
			this.naturals = nat;
		}
		return nat;
	}

	public StablePath getStablePath() {
		StablePath sp = this.stablePath;
		if (sp == null) {
			byte[] stablePathData = this.stablePathData;
			if (stablePathData == null) {
				return null;
			}
			sp = new StablePath(stablePathData);
			this.stablePath = sp;
		}
		return sp;
	}

	public StablePath getOrInitStablePath() {
		StablePath sp = this.stablePath;
		if (sp == null) {
			byte[] stablePathData = this.stablePathData;
			if (stablePathData == null) {
				stablePathData = new byte[4096];
				this.stablePathData = stablePathData;
			}
			sp = new StablePath(stablePathData);
			this.stablePath = sp;
		}
		return sp;
	}
}
