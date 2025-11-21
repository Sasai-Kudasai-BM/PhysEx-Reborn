package net.skds.physex.mixins._fluids.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BottleItem.class)
public abstract class BottleItemMixin extends Item {


	public BottleItemMixin(Properties properties) {
		super(properties);
	}

	@Redirect(method = "use", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"
	))
	public boolean use(FluidState fs,
					   TagKey<Fluid> tagKey,
					   @Local(argsOnly = true, type = Level.class) Level level,
					   @Local(type = BlockPos.class, name = "blockPos") BlockPos pos
	) {
		if (fs.is(tagKey)) {
			int amount = fs.getAmount();
			if (amount >= FluidUtils.LEVELS_IN_BOTTLE && fs.getType() instanceof FlowingFluid ff) {
				amount -= FluidUtils.LEVELS_IN_BOTTLE;
				FluidUtils.setFluid(level, pos, ff, amount, false);
				return true;
			}
		}
		return false;
	}

	@Redirect(method = "use", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"

	))
	public BlockHitResult use(Level level, Player player, ClipContext.Fluid fluid) {
		return getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
	}
}
