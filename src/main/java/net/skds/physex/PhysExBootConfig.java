package net.skds.physex;

import lombok.Getter;
import net.skds.lib2.io.codec.SosisonUtils;
import net.skds.lib2.io.codec.UniversalSerializer;
import net.skds.lib2.io.json.annotation.JsonComment;
import net.w3e.lib.utils.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@Getter
public final class PhysExBootConfig {
	public static final Path PATH = PhysEx.CFG_DIR.resolve("boot.json5");
	public static final PhysExBootConfig INSTANCE = load();

	@JsonComment("""
			
			\t+------------------------------------------+
			\t| Not-reloadable config with major tweaks  |
			\t| restart is required for changes to apply |
			\t+------------------------------------------+
			
			// Enables the fluids part
			""")
	private boolean fluidPhysicsEnabled = true;

	@JsonComment("Enables the blocks part")
	private boolean blockPhysicsEnabled = true;

	@JsonComment("Adds new logic layer for fluids to chunks (may cause stability issues)")
	private boolean extraFluidLayerEnabled = true;

	@JsonComment("""
			
			Describes behaviour of "waterlogged" blocks (slabs, fences, trapdoors etc...)
			values:
				ALL_OR_NOTHING (default) - Waterlogged state sets only for fully filled blocks
				FILL_AT_HALF - Waterlogged state sets only for at least half filled blocks
				FILL_ALWAYS - Waterlogged state sets if any amount of water presents in block
			""")
	private WaterlogPolicy waterlogPolicy = WaterlogPolicy.ALL_OR_NOTHING;

	@JsonComment("Makes this mod server side only")
	private ServerOnlyPolicy serverOnly = ServerOnlyPolicy.DEFAULT;


	public boolean isExtraFluidLayerEnabled() {
		return blockPhysicsEnabled && extraFluidLayerEnabled && !serverOnly.isEnabled();
	}

	@Getter
	public static final class ServerOnlyPolicy {
		@JsonComment("Actually enables server-only mode")
		private boolean enabled = false;
		//private WaterlogPolicy clientWaterlogPolicy = WaterlogPolicy.ALL_OR_NOTHING;

		private static final ServerOnlyPolicy DEFAULT = new ServerOnlyPolicy();
	}

	public enum WaterlogPolicy {
		ALL_OR_NOTHING,
		FILL_AT_HALF,
		FILL_ALWAYS
	}

	private static PhysExBootConfig load() {
		try {
			if (Files.exists(PATH)) {
				String text = Files.readString(PATH);
				PhysExBootConfig cfg = SosisonUtils.parseJson(text, PhysExBootConfig.class);
				Objects.requireNonNull(cfg);
				Objects.requireNonNull(cfg.serverOnly);
				//Objects.requireNonNull(cfg.serverOnly.clientWaterlogPolicy);
				return cfg;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		PhysExBootConfig cfg = new PhysExBootConfig();
		try {
			UniversalSerializer<PhysExBootConfig> serializer = SosisonUtils.getJson5Registry().getSerializer(PhysExBootConfig.class);
			String text = serializer.toJson(cfg);
			FileUtils.createParentDirs(PATH);
			Files.writeString(PATH, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return cfg;
	}
}
