package net.skds.physex.blockphysics.chunk;

import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.nbt.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Arrays;

import static net.skds.physex.PhysExUtils.isArrayEmpty;


public final class BPChunkAttachment {

	public static final Codec<BPChunkAttachment> CODEC = ExtraCodecs.NBT.xmap(
			BPChunkAttachment::fromNbt,
			BPChunkAttachment::toNbt
	);
	public static final String SECTIONS_KEY = "sections";
	public static final String NATURALS_KEY = "natural";
	public static final String STABLE_PATH_KEY = "stable_path";

	@Getter
	private BPChunkSection[] sections;

	public BPChunkAttachment() {
	}

	private BPChunkAttachment(BPChunkSection[] sections) {
		this.sections = sections;
	}

	public void checkHeight(ChunkAccess chunk) {
		int sl = chunk.getSections().length;
		if (this.sections == null) {
			this.sections = new BPChunkSection[sl];
			return;
		}
		if (this.sections.length != sl) {
			this.sections = Arrays.copyOf(this.sections, sl);
		}
	}

	static BPChunkAttachment fromNbt(Tag tag) {
		try {
			CompoundTag nbt = tag.asCompound().orElse(null);
			if (nbt == null || nbt.isEmpty()) return new BPChunkAttachment();
			ListTag sectionsTag = nbt.getListOrEmpty(SECTIONS_KEY);
			if (!sectionsTag.isEmpty()) {
				BPChunkSection[] sections = new BPChunkSection[sectionsTag.size()];
				for (int i = 0; i < sections.length; i++) {
					var op = sectionsTag.getCompound(i);
					if (op.isEmpty()) continue;
					CompoundTag sectionTag = op.get();
					long[] naturals = sectionTag.getLongArray(NATURALS_KEY).orElse(null);
					byte[] stablePath = sectionTag.getByteArray(STABLE_PATH_KEY).orElse(null);
					if (stablePath != null && stablePath.length != 4096) stablePath = null;
					sections[i] = new BPChunkSection(i, naturals, stablePath);
				}
				return new BPChunkAttachment(sections);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return new BPChunkAttachment();
	}

	CompoundTag toNbt() {
		CompoundTag root = new CompoundTag();
		final var sections = this.sections;
		ListTag sectionsTag = new ListTag();
		for (BPChunkSection section : sections) {
			CompoundTag sectionTag = new CompoundTag();
			if (section != null) {
				var nat = section.getNaturals();
				if (nat != null && !nat.isEmpty()) {
					sectionTag.put(NATURALS_KEY, new LongArrayTag(nat.toLongArray()));
				}
				var stp = section.getStablePathData();
				if (stp != null && !isArrayEmpty(stp)) {
					sectionTag.put(STABLE_PATH_KEY, new ByteArrayTag(stp));
				}
			}
			sectionsTag.add(sectionTag);
		}
		if (!sectionsTag.isEmpty()) {
			root.put(SECTIONS_KEY, sectionsTag);
		}
		return root;
	}
}
