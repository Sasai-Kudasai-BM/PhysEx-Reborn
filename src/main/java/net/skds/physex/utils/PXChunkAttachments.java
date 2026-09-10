package net.skds.physex.utils;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.skds.lib2.utils.exception.TODOException;
import net.skds.physex.ModLoader;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;
import net.skds.physex.fabric.PXFabricChunkAttachments;

public abstract class PXChunkAttachments {

	private static final class Holder {
		private static final PXChunkAttachments INSTANCE = createInstance();
	}

	private static PXChunkAttachments createInstance() {
		return switch (ModLoader.CURRENT) {
			case FABRIC -> new PXFabricChunkAttachments();
			case NEOFORGE -> throw new TODOException();
		};
	}

	public static PXChunkAttachments getInstance() {
		return Holder.INSTANCE;
	}

	public abstract BPChunkAttachment getBP(ChunkAccess chunk);

	public abstract BPChunkAttachment getOrCreateBP(ChunkAccess chunk);

	public abstract void markBPDirty(ChunkAccess chunk);

	public abstract void set(LevelChunk chunk, BPChunkAttachment attachment);


}
