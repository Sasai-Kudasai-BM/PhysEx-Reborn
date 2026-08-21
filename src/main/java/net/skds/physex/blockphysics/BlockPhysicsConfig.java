package net.skds.physex.blockphysics;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.skds.lib2.io.codec.SosisonUtils;
import net.skds.lib2.io.codec.UniversalSerializer;
import net.skds.lib2.io.json.annotation.JsonComment;
import net.skds.lib2.utils.SKDSFiles;
import net.skds.physex.PhysEx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@SuppressWarnings("FieldMayBeFinal")
@Getter
public class BlockPhysicsConfig {
	private static final Path PATH = PhysEx.CFG_DIR.resolve("block_physics.jsonc");

	private static final int VERSION = 3;

	private float arcCompressionThreshold = 1;
	private float arcTensileThreshold = .1f;
	private float arcTensileFactor = 1;
	private float beamFactor = .3f;
	private int longBeamThreshold = 5;
	private float hangThreshold = 1;
	private float slideFactor = 0.1f;
	private float reinforcedDirtFactor = 4f;

	@JsonComment("Block properties groups")
	private Group[] groups = {};

	@JsonComment("!!! Do not touch !!! (not for humans)")
	@Getter(AccessLevel.NONE)
	private int configVersion = 0;

	@Getter
	@NoArgsConstructor
	static class Group {
		private String tag;
		private String dimension = null;
		private boolean immovable = false;
		private boolean natural = false;
		private BlockPhysicsData physics;

		Group(TagKey<Block> tag, ResourceKey<Level> dimension, boolean immovable, boolean natural, BlockPhysicsData data) {
			this.tag = tag.location().toString();
			if (dimension != null) {
				this.dimension = dimension.identifier().toString();
			}
			this.immovable = immovable;
			this.natural = natural;
			this.physics = data;
		}

		public BlockPhysicsData getData(BlockPhysicsConfig config) {
			if (immovable) {
				return BlockPhysicsData.IMMOVABLE;
			}
			return physics;
		}
	}

	public static BlockPhysicsConfig load() {
		try {
			if (Files.exists(PATH)) {
				String text = Files.readString(PATH);
				BlockPhysicsConfig cfg = SosisonUtils.parseJson(text, BlockPhysicsConfig.class);
				Objects.requireNonNull(cfg);
				if (cfg.groups.length == 0) {
					cfg.groups = BuiltinPhysicsConfig.builtinGroups();
					cfg.configVersion = -1;
				}
				if (cfg.configVersion != VERSION) {
					cfg.configVersion = VERSION;
					save(cfg);
				}
				return cfg;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		BlockPhysicsConfig cfg = new BlockPhysicsConfig();
		cfg.groups = BuiltinPhysicsConfig.builtinGroups();
		cfg.configVersion = VERSION;
		save(cfg);
		return cfg;
	}


	private static void save(BlockPhysicsConfig cfg) {
		try {
			UniversalSerializer<BlockPhysicsConfig> serializer = SosisonUtils.getJsonCRegistry().getSerializer(BlockPhysicsConfig.class);
			String text = serializer.toJson(cfg);
			SKDSFiles.createFileAndParentDir(PATH, text);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
