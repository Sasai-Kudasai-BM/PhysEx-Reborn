package net.skds.physex.client;

import net.fabricmc.api.ClientModInitializer;
import net.skds.physex.PhysEx;
import net.skds.physex.PhysExBootConfig;

public class PhysExClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (PhysExBootConfig.INSTANCE.getServerOnly().isEnabled()) {
			throw new IllegalStateException(PhysEx.MOD_NAME + """
					 mod was configured for server-only mode but it presents on client side
					delete it from client or disable server-only mode
					"""
			);
		}
	}
}
