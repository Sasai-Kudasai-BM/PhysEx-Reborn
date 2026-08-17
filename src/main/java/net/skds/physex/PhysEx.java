package net.skds.physex;

import net.fabricmc.api.ModInitializer;
import net.skds.lib2.utils.logger.SKDSLogger;
import net.skds.physex.fluids.PhysExFluidGameRules;
import net.skds.physex.fluids.PhysExFluidItems;
import net.skds.physex.fluids.VanillaFluidTweaks;
import net.skds.physex.fluids.layer.FluidLayer;

import java.nio.file.Path;


public class PhysEx implements ModInitializer {

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
		//CommandRegistrationCallback.EVENT.register(
		//		(dispatcher, ignored, ignored1) ->
		//				FluidDebugCommand.create(dispatcher)
		//);
	}

	public static boolean fluidItemsEnabled() {
		return PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled() && !PhysExBootConfig.INSTANCE.isServerOnly();
	}
}
