package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(KelpBlock.class)
public class KelpBlockMixin {

	/**
	 * @author Sasai_Kudasai_BM
	 * @reason dupe fix
	 */
	@Overwrite
	public boolean canGrowInto(BlockState blockState) {
		return blockState.is(Blocks.WATER) && blockState.getFluidState().isSource();
	}
}
