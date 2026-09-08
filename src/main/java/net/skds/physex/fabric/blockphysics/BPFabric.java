package net.skds.physex.fabric.blockphysics;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;

public class BPFabric {

	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<BPChunkAttachment> CHUNK_ATTACHMENT_TYPE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, "block_physics_data"),
			b -> b.persistent(BPChunkAttachment.CODEC)
	);

}
