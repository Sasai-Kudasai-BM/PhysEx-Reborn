package net.skds.physex.blockphysics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.skds.physex.PhysEx;

public class BuiltinPhysicsConfig {

	public static final TagKey<Block> REINFORCED_DIRT_TAG = tag("reinforced_dirt");
	public static final TagKey<Block> STAND_STILL_PHYSICS_TAG = tag("stand_still_physics");
	public static final TagKey<Block> OVERWORLD_NATURAL_STONE_TAG = tag("overworld_natural/stone");
	public static final TagKey<Block> END_NATURAL_STONE_TAG = tag("end_natural/stone");
	public static final TagKey<Block> END_NATURAL_BRICKS_TAG = tag("end_natural/bricks");
	public static final TagKey<Block> NETHER_NATURAL_STONE_TAG = tag("nether_natural/stone");
	public static final TagKey<Block> NETHER_NATURAL_GLOWSTONE_TAG = tag("nether_natural/glowstone");
	public static final TagKey<Block> NETHER_NATURAL_SAND_TAG = tag("nether_natural/sand");
	public static final TagKey<Block> NETHER_NATURAL_BRICKS_TAG = tag("nether_natural/bricks");

	static BlockPhysicsConfig.Group[] builtinGroups() {
		return new BlockPhysicsConfig.Group[]{
				new BlockPhysicsConfig.Group(STAND_STILL_PHYSICS_TAG, null, false, false,
						new BlockPhysicsData(0, 0, 0, -1, -1, false, false, false)),

				new BlockPhysicsConfig.Group(REINFORCED_DIRT_TAG, null, false, false,
						new BlockPhysicsData(-1, -1, 0, -1, -1, false, false, false)),

				new BlockPhysicsConfig.Group(OVERWORLD_NATURAL_STONE_TAG, Level.OVERWORLD, false, true,
						new BlockPhysicsData(3, 2, 0, -1, -1, false, true, false)),

				new BlockPhysicsConfig.Group(END_NATURAL_STONE_TAG, Level.END, false, true,
						new BlockPhysicsData(0, 0, 0, -1, -1, false, false, true)),

				new BlockPhysicsConfig.Group(END_NATURAL_BRICKS_TAG, Level.END, false, true,
						new BlockPhysicsData(0, 10, 0, -1, -1, true, true, false)),

				new BlockPhysicsConfig.Group(NETHER_NATURAL_STONE_TAG, Level.NETHER, false, true,
						new BlockPhysicsData(0, 0, 0, -1, -1, false, false, true)),

				new BlockPhysicsConfig.Group(NETHER_NATURAL_GLOWSTONE_TAG, Level.NETHER, false, true,
						new BlockPhysicsData(0, 0, 0, -1, -1, false, false, true)),

				new BlockPhysicsConfig.Group(NETHER_NATURAL_SAND_TAG, Level.NETHER, false, true,
						new BlockPhysicsData(0, 0, 0, -1, -1, false, false, true)),

				new BlockPhysicsConfig.Group(NETHER_NATURAL_BRICKS_TAG, Level.NETHER, false, true,
						new BlockPhysicsData(0, 10, 0, -1, -1, true, true, false)),
		};
	}

	private static TagKey<Block> tag(String name) {
		return TagKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, name)
		);
	}
}
