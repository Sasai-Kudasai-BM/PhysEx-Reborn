package net.skds.physex.blockphysics;

import lombok.AllArgsConstructor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

@AllArgsConstructor
public enum ToolType {
	DEFAULT(1, 1, 800, 100, null),
	SHOVEL(.1f, .01f, 1200, 200, BlockTags.MINEABLE_WITH_SHOVEL),
	AXE(5, 2.5f, 600, 50, BlockTags.MINEABLE_WITH_AXE),
	PICKAXE(6, .5f, 1800, 200, BlockTags.MINEABLE_WITH_PICKAXE);

	private static final ToolType[] VALUES = values();
	private static final ToolType[] ITERABLE = Arrays.copyOfRange(VALUES, 1, VALUES.length);

	public final float compressionStrengthMultiplier;
	public final float tensileStrengthMultiplier;
	public final float densityBias;
	public final float densityFactor;
	public final TagKey<Block> tag;

	public static ToolType forState(BlockState state) {
		for (ToolType type : ITERABLE) {
			if (state.is(type.tag)) return type;
		}
		return DEFAULT;
	}
}
