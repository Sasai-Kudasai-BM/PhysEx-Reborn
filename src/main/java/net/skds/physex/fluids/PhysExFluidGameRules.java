package net.skds.physex.fluids;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.skds.physex.PhysEx;
import net.skds.physex.PhysExBootConfig;

public class PhysExFluidGameRules {

	public static final GameRule<Integer> TASK_LIMIT =
			createGamerule("task_limit", GameRuleBuilder.forInteger(10_000).minValue(500));

	public static final GameRule<Boolean> DRIPSTONE_FILL_CAULDRON =
			createGamerule("dripstone_fill_cauldron", GameRuleBuilder.forBoolean(false));

	public static final GameRule<Integer> FLUID_DISPLACEMENT_LIMIT =
			createGamerule("fluid_displacement_limit", GameRuleBuilder.forInteger(8).range(0, 32));

	public static final GameRule<Integer> WATER_HOT_EVAPORATION =
			createGamerule("water_hot_fluid_evaporation", GameRuleBuilder.forInteger(2).range(0, 8));

	public static final GameRule<Double> FARMLAND_WATER_INTAKE =
			createGamerule("farmland_water_intake", GameRuleBuilder.forDouble(0.01).range(-1D, 1D));

	private static <T> GameRule<T> createGamerule(String id, GameRuleBuilder<T> builder) {
		if (!PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled()) return null;
		return builder.category(GameRuleCategory.MISC)
				.buildAndRegister(Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, id));
	}

	public static void init() {
	}
}
