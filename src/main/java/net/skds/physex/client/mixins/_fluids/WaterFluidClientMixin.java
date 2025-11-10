package net.skds.physex.client.mixins._fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.skds.physex.fluids.FluidUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WaterFluid.class)
public class WaterFluidClientMixin {

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason Completely new way of ticking fluids
	 */
	@Overwrite
	public void animateTick(Level level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
		if (!fluidState.isSource() && randomSource.nextInt(64) == 0) {
			if (fluidState.getFlow(level, blockPos).lengthSqr() > FluidUtils.FLOW_THRESHOLD_SQR) {
				level.playLocalSound(
						blockPos.getX() + 0.5,
						blockPos.getY() + 0.5,
						blockPos.getZ() + 0.5,
						SoundEvents.WATER_AMBIENT,
						SoundSource.AMBIENT,
						randomSource.nextFloat() * 0.25F + 0.75F,
						randomSource.nextFloat() + 0.5F,
						false
				);
			}
		}
		if (randomSource.nextInt(10) == 0) {
			level.addParticle(
					ParticleTypes.UNDERWATER,
					blockPos.getX() + randomSource.nextDouble(),
					blockPos.getY() + randomSource.nextDouble() * fluidState.getHeight(level, blockPos),
					blockPos.getZ() + randomSource.nextDouble(),
					0.0,
					0.0,
					0.0
			);
		}
	}
}
