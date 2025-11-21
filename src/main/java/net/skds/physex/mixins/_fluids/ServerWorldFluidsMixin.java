package net.skds.physex.mixins._fluids;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.ticks.LevelTicks;
import net.skds.physex.fluids.CustomFluidTicks;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerWorldFluidsMixin extends Level {

	protected ServerWorldFluidsMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
	}

	@SuppressWarnings("unused")
	@Shadow
	private final LevelTicks<Fluid> fluidTicks = new CustomFluidTicks(this::isPositionTickingWithEntitiesLoaded, (ServerLevel) (Object) this);

	@Redirect(method = "tickFluid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
	))
	private FluidState tickFluid(BlockState instance, @Local(argsOnly = true, ordinal = 0, type = BlockPos.class) BlockPos blockPos) {
		return this.getFluidState(blockPos);
	}

	@Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"), cancellable = true)
	public void updatePOIOnBlockStateChange(BlockPos blockPos, BlockState blockState, BlockState blockState2, CallbackInfo ci) {
		if (!blockState.getFluidState().isEmpty() && !blockState2.getFluidState().isEmpty()) {
			ci.cancel();
		}
	}

	@Override
	public void scheduleTick(BlockPos blockPos, Fluid fluid, int i) {
		i = FluidUtils.modifyTickRate(i);
		this.getFluidTicks().schedule(this.createTick(blockPos, fluid, i));
	}

	@Shadow
	public abstract boolean isPositionTickingWithEntitiesLoaded(long l);
}
