package net.skds.physex.client.fluids;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@UtilityClass
public class ClientFluidUtils {

	private static final float NEAR_ZERO = 0.004f;

	@SuppressWarnings("deprecation")
	public static float calculateAverageHeight(BlockAndTintGetter world,
											   Fluid fluid,
											   float self,
											   float h1, float h2,
											   BlockPos offsetPos,
											   BlockPos startPos
											   //BlockState blockState,
											   //FluidState fluidState
	) {
		if (!(h2 >= 1.0F) && !(h1 >= 1.0F)) {
			float[] mid = new float[2];
			if (h2 > 0.0F || h1 > 0.0F) {
				float i;
				// ======= getHeight ======
				FluidState fs = world.getFluidState(offsetPos);
				if (fluid.isSame(fs.getType())) {
					FluidState fsU = world.getFluidState(offsetPos.above());
					i = fluid.isSame(fsU.getType()) ? 1.0F : fs.getOwnHeight();
				} else {
					BlockPos pd = offsetPos.below();
					FluidState fsD = world.getFluidState(pd);
					if (fluid.isSame(fsD.getType()) && !world.getBlockState(offsetPos).isSolid()) {
						return NEAR_ZERO;
					} else i = !world.getBlockState(offsetPos).isSolid() ? 0.0F : -1.0F;
				}
				// =========================
				if (i >= 1.0F) {
					return 1.0F;
				}
				addWeightedHeight(mid, i);
			}
			if (h2 <= 0.0F || h1 <= 0.0F) {
				BlockPos p2 = new BlockPos(offsetPos.getX(), offsetPos.getY(), startPos.getZ());
				BlockPos p2d = p2.below();
				FluidState fsD2 = world.getFluidState(p2d);
				if (h2 <= 0 && fsD2.getType().isSame(fluid) && !world.getBlockState(p2).isSolid()) {
					return NEAR_ZERO;
				}
				BlockPos p3 = new BlockPos(startPos.getX(), offsetPos.getY(), offsetPos.getZ());
				BlockPos p3d = p3.below();
				FluidState fsD3 = world.getFluidState(p3d);
				if (h1 <= 0 && fsD3.getType().isSame(fluid) && !world.getBlockState(p3).isSolid()) {
					return NEAR_ZERO;
				}
			}

			addWeightedHeight(mid, self);
			addWeightedHeight(mid, h2);
			addWeightedHeight(mid, h1);
			float height = mid[0] / mid[1];
			return Math.max(height, NEAR_ZERO);
		} else {
			return 1.0F;
		}
	}

	private static void addWeightedHeight(float[] fs, float f) {
		if (f >= 0.8F) {
			fs[0] += f * 10.0F;
			fs[1] += 10.0F;
		} else if (f != -1F) {
			fs[0] += f;
			fs[1]++;
		}
	}
}
