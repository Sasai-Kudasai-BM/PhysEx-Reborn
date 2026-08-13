package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(FallingBlockEntity.class)
public class FallingBlockFluidMixin {

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 3), slice = @Slice(
			from = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;hasProperty(Lnet/minecraft/world/level/block/state/properties/Property;)Z"
			),
			to = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
			)
	))
	int modifyFlags(int constant) {
		return constant | FluidUtils.DISPLACE_FLAG;
	}
}
