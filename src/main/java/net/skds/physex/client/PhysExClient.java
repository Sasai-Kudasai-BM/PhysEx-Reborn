package net.skds.physex.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.blockphysics.entity.BPFallingBlock;
import net.skds.physex.client.blockphysics.BPFallingBlockRenderer;

public class PhysExClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (PhysExBootConfig.INSTANCE.isBlockPhysicsEnabled()) {
			EntityRenderers.register(BPFallingBlock.TYPE, BPFallingBlockRenderer::new);
		}
	}
}
