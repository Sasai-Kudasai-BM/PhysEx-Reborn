package net.skds.physex;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

public class PhysExGameRules {

	public static final GameRules.Key<GameRules.IntegerValue> TASK_LIMIT =
			createFluidGamerule("taskLimit", GameRuleFactory.createIntRule(5000, 10));
	public static final GameRules.Key<GameRules.BooleanValue> DRIPSTONE_FILL_CAULDRON =
			createFluidGamerule("dripstoneFillCauldron", GameRuleFactory.createBooleanRule(false));


	private static <T extends GameRules.Value<T>> GameRules.Key<T> createFluidGamerule(String id, GameRules.Type<T> type) {
		if (!PhysExBootConfig.INSTANCE.isFluidPhysicsEnabled()) return null;
		return GameRuleRegistry.register(PhysEx.MOD_ID + "." + id, GameRules.Category.MISC, type);
	}

	public static void init() {
	}
}
