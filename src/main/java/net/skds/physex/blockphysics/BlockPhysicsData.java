package net.skds.physex.blockphysics;

import java.util.Locale;

public record BlockPhysicsData(
		int arc,
		int beam,
		int slideInstability,
		float mass,
		float durability,
		boolean longBeam,
		boolean hang,
		boolean vanillaPhysics
) {

	public static final BlockPhysicsData IMMOVABLE = new BlockPhysicsData(0, 0, 5000, 0, 0, false, false, false);
	public static final BlockPhysicsData AIR = new BlockPhysicsData(0, 0, 1, 0, 0, false, false, false);

	public static BlockPhysicsData merge(BlockPhysicsData original, BlockPhysicsData fallback, BlockPhysicsData changes) {
		if (original == null) original = fallback;
		return new BlockPhysicsData(
				changes.arc < 0 ? original.arc : changes.arc,
				changes.beam < 0 ? original.beam : changes.beam,
				changes.slideInstability < 0 ? original.slideInstability : changes.slideInstability,
				changes.mass < 0 ? original.mass : changes.mass,
				changes.durability < 0 ? original.durability : changes.durability,
				changes.longBeam,
				changes.hang,
				changes.vanillaPhysics
		);
	}

	public static BlockPhysicsData calcVanilla(float compressionStrength, float tensileStrength, float density, float volume) {
		float durability = compressionStrength + tensileStrength;
		float mass = density * volume;
		return new BlockPhysicsData(
				0,
				0,
				0,
				mass,
				durability,
				false,
				false,
				true
		);
	}

	public static BlockPhysicsData calc(BlockPhysicsConfig cfg, float compressionStrength, float tensileStrength, float density, float volume) {
		int arc = 0;
		float mass = density * volume;
		float normalizedCompression = compressionStrength * 1000 / mass;
		float normalizedTensile = tensileStrength * 1000 / mass;
		if (normalizedCompression > cfg.getArcCompressionThreshold()) {
			if (normalizedTensile > cfg.getArcTensileThreshold()) {
				arc = (int) Math.max(normalizedTensile / cfg.getArcTensileFactor(), 1);
			}
		}
		float beamStrength = Math.min(normalizedCompression, normalizedTensile);
		int beam = (int) (beamStrength * cfg.getBeamFactor());
		boolean longBeam = beam > cfg.getLongBeamThreshold();
		float durability = compressionStrength + tensileStrength;
		boolean hang = normalizedTensile > cfg.getHangThreshold();
		int slide = (int) (cfg.getSlideFactor() / normalizedTensile);
		if (slide > 5) slide = 5;
		if (arc > 3) arc = 3;
		arc *= 2;
		return new BlockPhysicsData(
				arc,
				beam,
				slide,
				mass,
				durability,
				longBeam,
				hang,
				false
		);
	}

	public String toStringFormatted() {
		if (isAir()) return "AIR";
		if (isImmovable()) return "IMMOVABLE";
		if (vanillaPhysics) return "Vanilla[" +
				"m=" + mass +
				String.format(Locale.US, ", dur=%.2f", durability) +
				"]";
		return "[" +
				"arc=" + arc +
				", bm=" + beam +
				", sld=" + slideInstability +
				", m=" + mass +
				String.format(Locale.US, ", dur=%.2f", durability) +
				", lng=" + longBeam +
				", hng=" + hang +
				"]";
	}

	public boolean haveLateralStrength() {
		return (arc > 0 || beam > 0) && slideInstability == 0;
	}

	public int lateralLimit() {
		return Math.max(this.arc, this.beam);
	}

	public boolean isNormal() {
		return this != AIR && this != IMMOVABLE && !vanillaPhysics;
	}

	public boolean isAir() {
		return this == AIR;
	}

	public boolean isImmovable() {
		return this == IMMOVABLE;
	}

	public int getDirMaskNoDown() {
		int mask = 0;
		if (haveLateralStrength()) {
			mask = BlockPhysicsUtils.DIR_HORIZONTAL_MASK;
		}
		if (hang) {
			mask |= BlockPhysicsUtils.DIR_UP_MASK;
		}
		return mask;
	}

	public int getDirMask() {
		return BlockPhysicsUtils.DIR_DOWN_MASK | getDirMaskNoDown();
	}
}
