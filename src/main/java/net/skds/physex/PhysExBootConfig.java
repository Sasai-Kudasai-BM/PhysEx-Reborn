package net.skds.physex;

import lombok.AccessLevel;
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
	public static final Path PATH = PhysEx.CFG_DIR.resolve("boot.jsonc");
	public static final PhysExBootConfig INSTANCE = load();

	private static final int VERSION = 1;
	private static final String COMMENT = """
			/*
			\t+------------------------------------------+
			\t| Not-reloadable config with major tweaks  |
			\t| restart is required for changes to apply |
			\t+------------------------------------------+
			*/
			""";

	@JsonComment("Enables the fluids part")
	private boolean fluidPhysicsEnabled = true;

	@JsonComment("""
			
				Adds new logic fluids layer to chunks (may cause stability issues)
				Allows variety of hollow blocks to be filled with fluids
				f.e. doors, anvils, chests, grass, fences
			""")
	private boolean extraFluidLayerEnabled = true;

	//@JsonComment("""
	//
	//		Describes behavior of "waterlogged" blocks (slabs, fences, trapdoors etc...)
	//		values:
	//			ALL_OR_NOTHING (default) - Waterlogged state sets only for fully filled blocks
	//			FILL_AT_HALF - Waterlogged state sets only for at least half filled blocks
	//			FILL_ALWAYS - Waterlogged state sets if any amount of water presents in block
	//		""")
	//private WaterlogPolicy waterlogPolicy = WaterlogPolicy.ALL_OR_NOTHING;


	@JsonComment("Enables the blocks part (WIP)")
	private transient boolean blockPhysicsEnabled = false;

	@JsonComment("Makes this mod server side only")
	private boolean serverOnly = false;

	@JsonComment("!!! FOR ADVANCED USERS AND MOD MAKERS	!!! The flag that telling placed block to try to displace fluid from it (FLAG = 1 << n)")
	private int fluidDisplaceFlagBitOffset = 23;

	@JsonComment("!!! Do not touch !!! (not for humans)")
	@Getter(AccessLevel.NONE)
	private int configVersion = 0;

	public boolean isExtraFluidLayerEnabled() {
		return fluidPhysicsEnabled && extraFluidLayerEnabled;
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
				if (cfg.fluidDisplaceFlagBitOffset < 10 || cfg.fluidDisplaceFlagBitOffset > 31)
					throw new IllegalArgumentException("fluidDisplaceFlagBitOffset is out of bounds [10;31]");
				//Objects.requireNonNull(cfg.serverOnly.clientWaterlogPolicy);

				if (cfg.configVersion != VERSION) {
					cfg.configVersion = VERSION;
					save(cfg);
				}
				return cfg;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		PhysExBootConfig cfg = new PhysExBootConfig();
		cfg.configVersion = VERSION;
		save(cfg);
		return cfg;
	}

	private static void save(PhysExBootConfig cfg) {
		try {
			UniversalSerializer<PhysExBootConfig> serializer = SosisonUtils.getJsonCRegistry().getSerializer(PhysExBootConfig.class);
			String text = serializer.toJson(cfg);
			FileUtils.createParentDirs(PATH);
			Files.writeString(PATH, COMMENT + text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
