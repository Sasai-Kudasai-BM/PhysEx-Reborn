package net.skds.physex.server;

import net.fabricmc.api.DedicatedServerModInitializer;

public class PhysExServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		System.out.println("onInitializeServer");
	}
}
