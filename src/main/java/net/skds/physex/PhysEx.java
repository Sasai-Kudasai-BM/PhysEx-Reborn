package net.skds.physex;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.skds.lib2.utils.logger.SKDSLogger;
import net.skds.physex.fluids.FluidDebugCommand;
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
		PhysExGameRules.init();
		if (PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled()) {
			VanillaFluidTweaks.init();
			if (PhysExBootConfig.INSTANCE.isExtraFluidLayerEnabled()) {
				FluidLayer.init();
			}
		}

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, _, _) ->
						FluidDebugCommand.create(dispatcher)
		);
	}
}
