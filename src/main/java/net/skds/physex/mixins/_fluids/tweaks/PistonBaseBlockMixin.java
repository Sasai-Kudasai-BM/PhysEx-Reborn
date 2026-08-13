package net.skds.physex.mixins._fluids.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

	@Inject(method = "moveBlocks", at = @At(value = "INVOKE",
			target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
			ordinal = 0
	), slice = @Slice(from = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z"
	)), cancellable = true)
	void moveBlocks(Level level,
	                BlockPos blockPos,
	                Direction direction,
	                boolean bl,
	                CallbackInfoReturnable<Boolean> cir,
	                @Local(type = PistonStructureResolver.class) PistonStructureResolver pistonStructureResolver
	) {
		if (!level.isClientSide() && !FluidUtils.pistonHook((ServerLevel) level, pistonStructureResolver, blockPos, direction)) {
			cir.setReturnValue(false);
		}
	}
}
