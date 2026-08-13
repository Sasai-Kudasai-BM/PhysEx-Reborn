package net.skds.physex;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

public class PhysExGameRules {

	public static final GameRules.Key<GameRules.IntegerValue> TASK_LIMIT =
			createFluidGamerule("TaskLimit", GameRuleFactory.createIntRule(5000, 10));

	public static final GameRules.Key<GameRules.BooleanValue> DRIPSTONE_FILL_CAULDRON =
			createFluidGamerule("DripstoneFillCauldron", GameRuleFactory.createBooleanRule(false));

	public static final GameRules.Key<GameRules.IntegerValue> FLUID_DISPLACEMENT_LIMIT =
			createFluidGamerule("FluidDisplacementLimit", GameRuleFactory.createIntRule(8, 0, 32));

	public static final GameRules.Key<GameRules.IntegerValue> WATER_HOT_EVAPORATION =
			createFluidGamerule("WaterHotFluidEvaporation", GameRuleFactory.createIntRule(2, 0, 8));

	public static final GameRules.Key<GameRules.IntegerValue> FARMLAND_WATER_INTAKE =
			createFluidGamerule("FarmlandWaterIntake", GameRuleFactory.createIntRule(10, 0, 1000));

	private static <T extends GameRules.Value<T>> GameRules.Key<T> createFluidGamerule(String id, GameRules.Type<T> type) {
		if (!PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled()) return null;
		return GameRuleRegistry.register(PhysEx.MOD_ID + id, GameRules.Category.MISC, type);
	}

	public static void init() {
	}
}
