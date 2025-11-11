package net.skds.physex.mixins._fluids._layer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockLayerMixin extends Block {

	public LiquidBlockLayerMixin(Properties properties) {
		super(properties);
	}

	@Final
	@Shadow
	protected FlowingFluid fluid;

	/**
	 * @author Sasai_kudasai_BM
	 * @reason new fluid behaviour
	 */
	@Overwrite
	public BlockState updateShape(
			BlockState blockState,
			LevelReader levelReader,
			ScheduledTickAccess scheduledTickAccess,
			BlockPos blockPos,
			Direction direction,
			BlockPos blockPos2,
			BlockState blockState2,
			RandomSource randomSource
	) {
		scheduledTickAccess.scheduleTick(blockPos, this.fluid, this.fluid.getTickDelay(levelReader));
		return super.updateShape(blockState, levelReader, scheduledTickAccess, blockPos, direction, blockPos2, blockState2, randomSource);
	}

	/// **
	// * @author Sasai_kudasai_BM
	// * @reason new fluid behaviour
	// */
	//@Overwrite
	//public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, @Nullable Orientation orientation, boolean bl) {
	//	this.shouldSpreadLiquid(level, blockPos, blockState);
	//}
	//@Shadow
	//protected abstract boolean shouldSpreadLiquid(Level level, BlockPos blockPos, BlockState blockState);
}
