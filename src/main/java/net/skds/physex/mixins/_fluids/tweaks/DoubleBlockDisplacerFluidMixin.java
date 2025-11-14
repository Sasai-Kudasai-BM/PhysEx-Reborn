package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin({DoublePlantBlock.class, DoorBlock.class})
public class DoubleBlockDisplacerFluidMixin {

	@ModifyConstant(method = "setPlacedBy", constant = @Constant(intValue = 3))
	int placeBlock(int constant) {
		return constant | FluidUtils.DISPLACE_FLAG;
	}
}
