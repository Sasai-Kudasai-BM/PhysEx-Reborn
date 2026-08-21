package net.skds.physex;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.skds.lib2.utils.logger.SKDSLogger;
import net.skds.physex.blockphysics.BlockPhysicsUtils;
import net.skds.physex.blockphysics.items.BlockPhysicsDebugItem;
import net.skds.physex.fluids.PhysExFluidGameRules;
import net.skds.physex.fluids.PhysExFluidItems;
import net.skds.physex.fluids.VanillaFluidTweaks;
import net.skds.physex.fluids.layer.FluidLayer;

import java.nio.file.Path;


public class PhysEx implements ModInitializer {

	public static final boolean DEBUG = Boolean.getBoolean("physex.debug");

	public static final String MOD_ID = "physex";
	public static final String MOD_NAME = "PhysEx";
	public static final Path CFG_DIR = Path.of("config", MOD_ID);
	public static final SKDSLogger LOGGER = new SKDSLogger(MOD_NAME);

	@Override
	public void onInitialize() {
		PhysExFluidGameRules.init();
		if (PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled()) {
			VanillaFluidTweaks.init();
			if (PhysExBootConfig.INSTANCE.isExtraFluidLayerEnabled()) {
				FluidLayer.init();
			}
		}
		if (fluidItemsEnabled()) {
			PhysExFluidItems.init();
		}
		if (PhysExBootConfig.INSTANCE.isBlockPhysicsEnabled()) {
			ServerLifecycleEvents.SERVER_STARTED.register((BlockPhysicsUtils::reload));
			ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(((server, resourceManager, success) -> {
				if (success) {
					BlockPhysicsUtils.reload(server);
				}
			}));
			if (DEBUG && !PhysExBootConfig.INSTANCE.isServerOnly()) {
				BlockPhysicsDebugItem.reg();
			}
		}
	}

	public static boolean fluidItemsEnabled() {
		return PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled() && !PhysExBootConfig.INSTANCE.isServerOnly();
	}
}
