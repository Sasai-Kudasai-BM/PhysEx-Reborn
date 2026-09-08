package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.skds.lib2.utils.collection.Deduplicator;
import net.skds.lib2.utils.collection.HashDeduplicator;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;
import net.skds.physex.blockphysics.chunk.BPChunkSection;
import net.skds.physex.blockphysics.path.StablePath;
import net.skds.physex.utils.PXChunkAttachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@UtilityClass
public class BlockPhysicsUtils {

	public static final BlockState AIR = Blocks.AIR.defaultBlockState();
	public static final Direction[] DIR_HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	public static final Direction[] DIRECTIONS = Direction.values();

	public static final int DIR_UP_MASK = 1 << Direction.UP.ordinal();
	public static final int DIR_DOWN_MASK = 1 << Direction.DOWN.ordinal();
	public static final int DIR_X_AXIS_MASK = (1 << Direction.WEST.ordinal()) | (1 << Direction.EAST.ordinal());
	public static final int DIR_Z_AXIS_MASK = (1 << Direction.SOUTH.ordinal()) | (1 << Direction.NORTH.ordinal());

	public static final int DIR_HORIZONTAL_MASK = (1 << Direction.NORTH.ordinal())
			| (1 << Direction.SOUTH.ordinal())
			| (1 << Direction.EAST.ordinal())
			| (1 << Direction.WEST.ordinal());

	public static final int DIR_ALL_MASK = ~(-1 << 6);

	public static final TagKey<Block> VANILLA_BLOCK_PHYSICS_TAG = TagKey.create(Registries.BLOCK,
			Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, "vanilla_block_physics")
	);

	public static final TagKey<Block> BREAK_ON_FALL_TAG = TagKey.create(Registries.BLOCK,
			Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, "break_on_fall")
	);

	public static BlockPhysicsData getPhysics(ServerLevel world, BlockPos pos, BlockState state) {
		return BlockStatePhysicsHolder.get(state, world, isNatural(world, pos));
	}

	public static void onBlockSet(ServerLevel world, LevelChunk chunk, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		if (oldState == newState) return;
		boolean natural = (flags & Block.UPDATE_KNOWN_SHAPE) != 0;
		BPChunkSection section;
		if (natural) {
			section = getSection(world, pos, chunk);
		} else {
			section = getOrCreateSection(world, pos, chunk);
			if (oldState == null || oldState.getBlock() != newState.getBlock() || oldState.getFluidState() == newState.getFluidState()) {
				BlockPhysicsManager.get(world).blockUpdated(pos);
			}
		}
		if (section != null) {
			section.setNatural(pos.getX(), pos.getY(), pos.getZ(), natural);
		}
	}

	public static boolean isNatural(ServerLevel world, BlockPos pos) {
		BPChunkSection section = getSection(world, pos);
		if (section == null) return true;
		return section.isNatural(pos.getX(), pos.getY(), pos.getZ());
	}

	public static void setNatural(ServerLevel world, BlockPos pos, boolean natural) {
		BPChunkSection section = getOrCreateSection(world, pos);
		section.setNatural(pos.getX(), pos.getY(), pos.getZ(), natural);
	}

	public static StablePath getStablePath(ServerLevel world, BlockPos pos) {
		BPChunkSection section = getSection(world, pos);
		if (section == null) return null;
		return section.getStablePath();
	}

	public static StablePath getOrCreateStablePath(ServerLevel world, BlockPos pos) {
		BPChunkSection section = getOrCreateSection(world, pos);
		return section.getOrInitStablePath();
	}

	public static BPChunkSection getSection(ServerLevel world, BlockPos pos) {
		return getSection(world, pos, world.getChunkAt(pos));
	}

	public static BPChunkSection getSection(ServerLevel world, BlockPos pos, LevelChunk chunk) {
		BPChunkAttachment attachment = PXChunkAttachments.getInstance().getBP(chunk);
		if (attachment == null) return null;
		return attachment.getSections()[world.getSectionIndex(pos.getY())];
	}

	public static BPChunkSection getOrCreateSection(ServerLevel world, BlockPos pos) {
		return getOrCreateSection(world, pos, world.getChunkAt(pos));
	}

	public static BPChunkSection getOrCreateSection(ServerLevel world, BlockPos pos, LevelChunk chunk) {
		BPChunkAttachment attachment = PXChunkAttachments.getInstance().getOrCreateBP(chunk);
		var sections = attachment.getSections();
		int index = world.getSectionIndex(pos.getY());
		BPChunkSection section = sections[index];
		if (section == null) {
			section = new BPChunkSection(index);
			sections[index] = section;
		}
		return section;
	}

	public static boolean checkDirection(int mask, Direction dir) {
		int dm = 1 << dir.ordinal();
		return (mask & dm) != 0;
	}

	public static int removeDirection(int mask, Direction dir) {
		int dm = 1 << dir.ordinal();
		return mask & ~dm;
	}

	public static int sectionIndex(int x, int y, int z) {
		return ((y & 15) << 8) | ((x & 15) << 4) | (z & 15);
	}

	public static int sectionIndex(BlockPos pos) {
		return ((pos.getY() & 15) << 8) | ((pos.getX() & 15) << 4) | (pos.getZ() & 15);
	}

	private static void applyConfig(MinecraftServer server, BlockPhysicsConfig config) {
		HashMap<BlockStatePhysicsHolder, Set<BlockState>> dataMap = new HashMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				var physicsData = getProperties(config, state);
				BlockStatePhysicsHolder holder = new BlockStatePhysicsHolder(physicsData);
				Set<BlockState> states;
				if ((states = dataMap.get(holder)) == null) {
					states = new ReferenceOpenHashSet<>();
					dataMap.put(holder, states);
				}
				states.add(state);
			}
		}
		dataMap.forEach(((data, blockStates) -> {
			for (BlockState state : blockStates) {
				((BlockStatePhysicsAccessor) state).physEx$setHolder(data);
			}
		}));
		Registry<Block> blockRegistry = server.registryAccess().lookupOrThrow(BuiltInRegistries.BLOCK.key());
		BlockPhysicsConfig.Group[] groups = config.getGroups();
		for (int i = groups.length - 1; i >= 0; i--) {
			try {
				BlockPhysicsConfig.Group group = groups[i];
				String tagName = group.getTag().replace("#", "");
				TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(tagName));
				BlockPhysicsData data = group.getData(config);
				ServerLevel dimension = null;
				if (group.getDimension() != null) {
					dimension = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(group.getDimension())));
				}
				boolean natural = group.isNatural();
				ArrayList<BlockState> states = new ArrayList<>();
				for (var bHolder : blockRegistry.getTagOrEmpty(tag)) {
					Block block = bHolder.value();
					states.addAll(block.getStateDefinition().getPossibleStates());
				}
				Deduplicator<BlockStatePhysicsHolder> deduplicator = new HashDeduplicator<>();
				for (BlockState state : states) {
					BlockStatePhysicsHolder holder = ((BlockStatePhysicsAccessor) state).physEx$getHolder();
					holder = deduplicator.addOrTake(merge(holder, data, dimension, natural));
					((BlockStatePhysicsAccessor) state).physEx$setHolder(holder);
				}
			} catch (Exception e) {
				System.err.println("BlockPhysicsConfig group error, skipping group");
				e.printStackTrace(System.err);
			}
		}
	}

	@SuppressWarnings("Java8MapApi")
	private static BlockStatePhysicsHolder merge(BlockStatePhysicsHolder holder, BlockPhysicsData data, ServerLevel dimension, boolean natural) {
		BlockStatePhysicsHolder newHolder;
		if (dimension == null) {
			if (natural) {
				newHolder = new BlockStatePhysicsHolder(
						holder.physicsData(),
						BlockPhysicsData.merge(holder.physicsDataNatural(), holder.physicsData(), data),
						holder.dimensions(),
						holder.dimensionsNatural()
				);
			} else {
				newHolder = new BlockStatePhysicsHolder(
						BlockPhysicsData.merge(holder.physicsData(), null, data),
						holder.physicsDataNatural(),
						holder.dimensions(),
						holder.dimensionsNatural()
				);
			}
		} else {
			if (natural) {
				var dims = holder.dimensionsNatural();
				if (dims == null) {
					dims = new Reference2ObjectOpenHashMap<>(4, .5f);
				}
				BlockPhysicsData d = dims.get(dimension);
				dims.put(dimension, BlockPhysicsData.merge(d, holder.physicsData(), data));
				newHolder = new BlockStatePhysicsHolder(
						holder.physicsData(),
						holder.physicsDataNatural(),
						holder.dimensions(),
						dims
				);
			} else {
				var dims = holder.dimensions();
				if (dims == null) {
					dims = new Reference2ObjectOpenHashMap<>(4, .5f);
				}
				BlockPhysicsData d = dims.get(dimension);
				dims.put(dimension, BlockPhysicsData.merge(d, holder.physicsData(), data));
				newHolder = new BlockStatePhysicsHolder(
						holder.physicsData(),
						holder.physicsDataNatural(),
						dims,
						holder.dimensionsNatural()
				);
			}
		}
		return newHolder;
	}

	@SuppressWarnings("deprecation")
	private static BlockPhysicsData getProperties(BlockPhysicsConfig config, BlockState blockState) {
		if (blockState.isAir() || blockState.liquid()) return BlockPhysicsData.AIR;
		float explosion = blockState.getBlock().getExplosionResistance();
		if (explosion < 0 || explosion > 999999) {
			if (blockState.canBeReplaced() || blockState.is(BlockTags.REPLACEABLE)) {
				return BlockPhysicsData.AIR;
			}
			return BlockPhysicsData.IMMOVABLE;
		}
		ToolType toolType = ToolType.forState(blockState);
		float density = toolType.densityBias + toolType.densityFactor * explosion;
		if (density > 10000) density = 10000;
		float strength = (float) (5 * Math.log(explosion + 1));
		float compressionStrength = strength * toolType.compressionStrengthMultiplier;
		float tensileStrength = strength * toolType.tensileStrengthMultiplier;
		if (explosion == 0 || blockState.is(VANILLA_BLOCK_PHYSICS_TAG)) {
			return BlockPhysicsData.calcVanilla(compressionStrength, tensileStrength, density, 1);
		}
		return BlockPhysicsData.calc(config, compressionStrength, tensileStrength, density, 1);
	}

	public static void reload(MinecraftServer server) {
		applyConfig(server, BlockPhysicsConfig.load());
	}
}
