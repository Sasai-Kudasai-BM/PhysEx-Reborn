package net.skds.physex.mixins._fluids.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoat.class)
public abstract class BoatMixin extends VehicleEntity {

	@Shadow
	private AbstractBoat.Status status;

	@Shadow
	public abstract float getWaterLevelAbove();

	@Shadow
	private AbstractBoat.Status oldStatus;

	public BoatMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Redirect(method = "checkInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(D)I", ordinal = 1))
	int checkInWater(double d, @Local(type = AABB.class, ordinal = 0) AABB aABB) {
		return Mth.ceil(aABB.maxY + 0.001);
	}

	@ModifyVariable(method = "isUnderwater", at = @At(value = "STORE"), ordinal = 0)
	private AABB isUnderWater(AABB aabb) {
		return aabb.expandTowards(0, .2, 0);
	}
	
	@Inject(method = "floatBoat", at = @At("HEAD"))
	private void floatBoat(CallbackInfo ci) {
		if (oldStatus == AbstractBoat.Status.IN_WATER || status == AbstractBoat.Status.IN_WATER) {
			double g = this.getWaterLevelAbove() - this.getBbHeight() - FluidUtils.BOAT_FLOAT_OFFSET - this.getY();
			if (g > 0) {
				this.setDeltaMovement(this.getDeltaMovement().add(0, g * FluidUtils.BOAT_FLOATABILITY, 0));
			}
		}
	}
}
