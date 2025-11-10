package net.skds.physex.client.mixins._fluids._layer;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.material.FluidState;
import net.skds.physex.client.fluids.layer.SectionCopyExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionFluidMixin {

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason new renderer
	 */
	@Overwrite
	public FluidState getFluidState(BlockPos blockPos) {
		int x = blockPos.getX();
		int y = blockPos.getY();
		int z = blockPos.getZ();
		return ((SectionCopyExtension) this.getSection(
				SectionPos.blockToSectionCoord(x),
				SectionPos.blockToSectionCoord(y),
				SectionPos.blockToSectionCoord(z)
		)).physEx$getFluidState(x, y, z);
	}

	@Shadow
	abstract SectionCopy getSection(int i, int j, int k);
}
