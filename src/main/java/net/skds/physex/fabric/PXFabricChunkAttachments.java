package net.skds.physex.fabric;

import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;
import net.skds.physex.fabric.blockphysics.BPFabric;
import net.skds.physex.utils.PXChunkAttachments;

@SuppressWarnings("UnstableApiUsage")
public final class PXFabricChunkAttachments extends PXChunkAttachments {

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
	public void markBPDirty(ChunkAccess chunk) {
		((AttachmentTargetImpl) chunk).fabric_markChanged(BPFabric.CHUNK_ATTACHMENT_TYPE);
	}

	@Override
	public void set(LevelChunk chunk, BPChunkAttachment attachment) {
		chunk.setAttached(BPFabric.CHUNK_ATTACHMENT_TYPE, attachment);
	}

	@Override
	public void init() {
		if (PhysExBootConfig.INSTANCE.isBlockPhysicsEnabled()) {
			AttachmentRegistryImpl.register(BPFabric.CHUNK_ATTACHMENT_TYPE.identifier(), BPFabric.CHUNK_ATTACHMENT_TYPE);
		}
	}

}
