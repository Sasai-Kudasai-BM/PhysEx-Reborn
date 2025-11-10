package net.skds.physex.fluids;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public record FluidStatePaletteFactory(
		Strategy<FluidState> fluidStatesStrategy,
		FluidState defaultFluidState,
		Codec<PalettedContainer<FluidState>> fluidStatesContainerCodec
) {
	public static FluidStatePaletteFactory create() {
		Strategy<FluidState> strategy = Strategy.createForBlockStates(Fluid.FLUID_STATE_REGISTRY);
		FluidState fluidState = Fluids.EMPTY.defaultFluidState();
		return new FluidStatePaletteFactory(
				strategy,
				fluidState,
				PalettedContainer.codecRW(FluidState.CODEC, strategy, fluidState)
		);
	}

	public PalettedContainer<FluidState> createForFluidStates() {
		return new PalettedContainer<>(this.defaultFluidState, this.fluidStatesStrategy);
	}
}
