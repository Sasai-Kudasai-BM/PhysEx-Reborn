package net.skds.physex.fabric;

import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;
import net.skds.physex.fabric.blockphysics.BPFabric;
import net.skds.physex.utils.PXChunkAttachments;

@SuppressWarnings("UnstableApiUsage")
public final class PXFabricChunkAttachments extends PXChunkAttachments {

	public PXFabricChunkAttachments() {
		if (PhysExBootConfig.INSTANCE.isBlockPhysicsEnabled()) {
			AttachmentRegistryImpl.register(BPFabric.CHUNK_ATTACHMENT_TYPE.identifier(), BPFabric.CHUNK_ATTACHMENT_TYPE);
		}
	}

	@Override
	public BPChunkAttachment getBP(ChunkAccess chunk) {
		BPChunkAttachment attachment = chunk.getAttached(BPFabric.CHUNK_ATTACHMENT_TYPE);
		if (attachment != null) {
			attachment.checkHeight(chunk);
		}
		return attachment;
	}

	@Override
	public BPChunkAttachment getOrCreateBP(ChunkAccess chunk) {
		BPChunkAttachment attachment = chunk.getAttached(BPFabric.CHUNK_ATTACHMENT_TYPE);
		if (attachment == null) {
			attachment = new BPChunkAttachment();
			chunk.setAttached(BPFabric.CHUNK_ATTACHMENT_TYPE, attachment);
		}
		attachment.checkHeight(chunk);
		return attachment;
	}

	@Override
	public void updateBP(ChunkAccess chunk) {
		BPChunkAttachment old = chunk.setAttached(BPFabric.CHUNK_ATTACHMENT_TYPE, null);
		if (old != null) {
			chunk.setAttached(BPFabric.CHUNK_ATTACHMENT_TYPE, old);
		}
	}

	@Override
	public void set(LevelChunk chunk, BPChunkAttachment attachment) {
		chunk.setAttached(BPFabric.CHUNK_ATTACHMENT_TYPE, attachment);
	}

}
