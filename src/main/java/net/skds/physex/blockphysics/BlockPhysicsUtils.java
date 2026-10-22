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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skds.lib2.mat.FastMath;
import net.skds.lib2.utils.collection.Deduplicator;
import net.skds.lib2.utils.collection.HashDeduplicator;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.chunk.BPChunkAttachment;
import net.skds.physex.blockphysics.chunk.BPChunkSection;
import net.skds.physex.blockphysics.entity.BPFallingBlock;
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

	private static final double EPSILON = 1E-10;
	private static final Direction[] DIRECTIONS_X = {Direction.EAST, null, Direction.WEST};
	private static final Direction[] DIRECTIONS_Y = {Direction.UP, null, Direction.DOWN};
	private static final Direction[] DIRECTIONS_Z = {Direction.SOUTH, null, Direction.NORTH};

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
			BlockPhysicsManager.get(world).popStable(pos);
		}
	}

	public static boolean isNatural(ServerLevel world, BlockPos pos) {
		BPChunkSection section = getSection(world, pos);
		if (section == null) return true;
		return section.isNatural(pos.getX(), pos.getY(), pos.getZ());
	}

	//public static void setNatural(ServerLevel world, BlockPos pos, boolean natural) {
	//	BPChunkSection section = getOrCreateSection(world, pos);
	//	section.setNatural(pos.getX(), pos.getY(), pos.getZ(), natural);
	//}

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

	//public static void markSectionDirty(LevelChunk chunk) {
	//	PXChunkAttachments.getInstance().markBPDirty(chunk);
	//}

	public static void markSectionDirty(ServerLevel world, BlockPos pos) {
		PXChunkAttachments.getInstance().markBPDirty(world.getChunk(pos));
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

	public static AABB getBroadAABB(BPFallingBlock fallingBlock) {
		return getBroadAABB(fallingBlock.getX(), fallingBlock.getY(), fallingBlock.getZ(), fallingBlock.getRelV());
	}

	public static AABB getBroadAABB(double x, double y, double z, Vec3 move) {
		double minX = x - 0.5 - EPSILON;
		double minY = y - EPSILON;
		double minZ = z - 0.5 - EPSILON;
		double resMinX = minX + Math.min(move.x, 0.0);
		double resMaxX = minX + 1.0 + EPSILON * 2 + Math.max(move.x, 0.0);
		double resMinY = minY + Math.min(move.y, 0.0);
		double resMaxY = minY + 1.0 + EPSILON * 2 + Math.max(move.y, 0.0);
		double resMinZ = minZ + Math.min(move.z, 0.0);
		double resMaxZ = minZ + 1.0 + EPSILON * 2 + Math.max(move.z, 0.0);
		return new AABB(resMinX, resMinY, resMinZ, resMaxX, resMaxY, resMaxZ);
	}

	public interface WorldCollisionChecker {
		boolean isSolid(int x, int y, int z);
	}

	public record CollisionResult(double tHit, Direction normal, double depth) {

	}

	public static CollisionResult traverseAABB(
			double startX, double startY, double startZ,
			double dx, double dy, double dz,
			double halfSize, WorldCollisionChecker world) {

		boolean xSmall = Math.abs(dx) < EPSILON;
		boolean ySmall = Math.abs(dy) < EPSILON;
		boolean zSmall = Math.abs(dz) < EPSILON;

		if (xSmall && ySmall && zSmall) {
			return null;
		}

		int signX = FastMath.sign(dx);
		int signY = FastMath.sign(dy);
		int signZ = FastMath.sign(dz);

		int voxelX = FastMath.floor(startX);
		int voxelY = FastMath.floor(startY);
		int voxelZ = FastMath.floor(startZ);

		double invDx = 1.0 / dx;
		double invDy = 1.0 / dy;
		double invDz = 1.0 / dz;

		double stepX = Math.abs(invDx);
		double stepY = Math.abs(invDy);
		double stepZ = Math.abs(invDz);

		double offsetX = (signX + 1) >>> 1;
		double offsetY = (signY + 1) >>> 1;
		double offsetZ = (signZ + 1) >>> 1;

		double wayX = ((voxelX + offsetX) - startX) * invDx;
		double wayY = ((voxelY + offsetY) - startY) * invDy;
		double wayZ = ((voxelZ + offsetZ) - startZ) * invDz;

		wayX = xSmall ? Double.MAX_VALUE : wayX;
		wayY = ySmall ? Double.MAX_VALUE : wayY;
		wayZ = zSmall ? Double.MAX_VALUE : wayZ;

		stepX = xSmall ? Double.MAX_VALUE : stepX;
		stepY = ySmall ? Double.MAX_VALUE : stepY;
		stepZ = zSmall ? Double.MAX_VALUE : stepZ;

		double tHit = 0.0;

		final double offsetMin = -halfSize + EPSILON;
		final double offsetMax = halfSize - EPSILON;

		int lastMinX = Integer.MAX_VALUE, lastMaxX = Integer.MIN_VALUE;
		int lastMinY = Integer.MAX_VALUE, lastMaxY = Integer.MIN_VALUE;
		int lastMinZ = Integer.MAX_VALUE, lastMaxZ = Integer.MIN_VALUE;
		boolean hasLastStep = false;

		while (true) {
			double currentCenterX = startX + dx * tHit;
			double currentCenterY = startY + dy * tHit;
			double currentCenterZ = startZ + dz * tHit;

			int minX = FastMath.floor(currentCenterX + offsetMin);
			int maxX = FastMath.floor(currentCenterX + offsetMax);
			int minY = FastMath.floor(currentCenterY + offsetMin);
			int maxY = FastMath.floor(currentCenterY + offsetMax);
			int minZ = FastMath.floor(currentCenterZ + offsetMin);
			int maxZ = FastMath.floor(currentCenterZ + offsetMax);

			CollisionResult closestCollision = null;

			for (int x = minX; x <= maxX; x++) {
				boolean inLastX = hasLastStep && (x >= lastMinX && x <= lastMaxX);
				for (int y = minY; y <= maxY; y++) {
					boolean inLastY = inLastX && (y >= lastMinY && y <= lastMaxY);
					for (int z = minZ; z <= maxZ; z++) {
						if (inLastY && (z >= lastMinZ && z <= lastMaxZ)) {
							continue;
						}
						if (world.isSolid(x, y, z)) {
							CollisionResult result = sweptAABB(startX, startY, startZ, dx, dy, dz, halfSize, x, y, z);
							if (result != null) {
								if (closestCollision == null || result.tHit() < closestCollision.tHit()) {
									closestCollision = result;
								}
							}
						}
					}
				}
			}
			if (closestCollision != null) {
				return closestCollision;
			}
			lastMinX = minX;
			lastMaxX = maxX;
			lastMinY = minY;
			lastMaxY = maxY;
			lastMinZ = minZ;
			lastMaxZ = maxZ;
			hasLastStep = true;

			if (tHit >= 1.0) {
				break;
			}
			if (wayX < wayY) {
				if (wayX < wayZ) {
					tHit = wayX;
					wayX += stepX;
					voxelX += signX;
				} else {
					tHit = wayZ;
					wayZ += stepZ;
					voxelZ += signZ;
				}
			} else {
				if (wayY < wayZ) {
					tHit = wayY;
					wayY += stepY;
					voxelY += signY;
				} else {
					tHit = wayZ;
					wayZ += stepZ;
					voxelZ += signZ;
				}
			}
		}
		return null;
	}

	public static CollisionResult sweptAABB(AABB b1, AABB b2, double dx, double dy, double dz) {
		double xInvEntry = dx > 0 ? b2.minX - b1.maxX : b2.maxX - b1.minX;
		double xInvExit = dx > 0 ? b2.maxX - b1.minX : b2.minX - b1.maxX;
		double yInvEntry = dy > 0 ? b2.minY - b1.maxY : b2.maxY - b1.minY;
		double yInvExit = dy > 0 ? b2.maxY - b1.minY : b2.minY - b1.maxY;
		double zInvEntry = dz > 0 ? b2.minZ - b1.maxZ : b2.maxZ - b1.minZ;
		double zInvExit = dx > 0 ? b2.maxZ - b1.minZ : b2.minZ - b1.maxZ;

		double xEntry = (Math.abs(dx) < EPSILON) ? Double.NEGATIVE_INFINITY : xInvEntry / dx;
		double xExit = (Math.abs(dx) < EPSILON) ? Double.POSITIVE_INFINITY : xInvExit / dx;
		double yEntry = (Math.abs(dy) < EPSILON) ? Double.NEGATIVE_INFINITY : yInvEntry / dy;
		double yExit = (Math.abs(dy) < EPSILON) ? Double.POSITIVE_INFINITY : yInvExit / dy;
		double zEntry = (Math.abs(dz) < EPSILON) ? Double.NEGATIVE_INFINITY : zInvEntry / dz;
		double zExit = (Math.abs(dz) < EPSILON) ? Double.POSITIVE_INFINITY : zInvExit / dz;

		double entryTime = Math.max(xEntry, Math.max(yEntry, zEntry));
		double exitTime = Math.min(xExit, Math.min(yExit, zExit));

		if (entryTime > exitTime || exitTime < 0.0 || entryTime > 1.0) {
			return null;
		}
		Direction normal;
		double depth;

		if (entryTime < 0.0) {
			double overlapX = Math.min(b1.maxX, b2.maxX) - Math.max(b1.minX, b2.minX);
			double overlapY = Math.min(b1.maxY, b2.maxY) - Math.max(b1.minY, b2.minY);
			double overlapZ = Math.min(b1.maxZ, b2.maxZ) - Math.max(b1.minZ, b2.minZ);

			if (overlapX < overlapY && overlapX < overlapZ) {
				depth = overlapX;
				normal = DIRECTIONS_X[FastMath.sign(dx) + 1];
			} else if (overlapY < overlapZ) {
				depth = overlapY;
				normal = DIRECTIONS_Y[FastMath.sign(dy) + 1];
			} else {
				depth = overlapZ;
				normal = DIRECTIONS_Z[FastMath.sign(dz) + 1];
			}
			entryTime = 0.0;
		} else {
			if (entryTime == xEntry) {
				normal = DIRECTIONS_X[FastMath.sign(dx) + 1];
			} else if (entryTime == yEntry) {
				normal = DIRECTIONS_Y[FastMath.sign(dy) + 1];
			} else {
				normal = DIRECTIONS_Z[FastMath.sign(dz) + 1];
			}
			depth = 0.0;
		}

		return new CollisionResult(entryTime, normal, depth);
	}


	private static CollisionResult sweptAABB(
			double startX, double startY, double startZ,
			double dx, double dy, double dz, double halfSize,
			int voxelX, int voxelY, int voxelZ) {

		double boxMaxX = voxelX + 1.0;
		double boxMaxY = voxelY + 1.0;
		double boxMaxZ = voxelZ + 1.0;

		double movingMinX = startX - halfSize;
		double movingMaxX = startX + halfSize;
		double movingMinY = startY - halfSize;
		double movingMaxY = startY + halfSize;
		double movingMinZ = startZ - halfSize;
		double movingMaxZ = startZ + halfSize;

		double xInvEntry = dx > 0 ? (double) voxelX - movingMaxX : boxMaxX - movingMinX;
		double xInvExit = dx > 0 ? boxMaxX - movingMinX : (double) voxelX - movingMaxX;
		double yInvEntry = dy > 0 ? (double) voxelY - movingMaxY : boxMaxY - movingMinY;
		double yInvExit = dy > 0 ? boxMaxY - movingMinY : (double) voxelY - movingMaxY;
		double zInvEntry = dz > 0 ? (double) voxelZ - movingMaxZ : boxMaxZ - movingMinZ;
		double zInvExit = dz > 0 ? boxMaxZ - movingMinZ : (double) voxelZ - movingMaxZ;

		double xEntry = (Math.abs(dx) < EPSILON) ? Double.NEGATIVE_INFINITY : xInvEntry / dx;
		double xExit = (Math.abs(dx) < EPSILON) ? Double.POSITIVE_INFINITY : xInvExit / dx;
		double yEntry = (Math.abs(dy) < EPSILON) ? Double.NEGATIVE_INFINITY : yInvEntry / dy;
		double yExit = (Math.abs(dy) < EPSILON) ? Double.POSITIVE_INFINITY : yInvExit / dy;
		double zEntry = (Math.abs(dz) < EPSILON) ? Double.NEGATIVE_INFINITY : zInvEntry / dz;
		double zExit = (Math.abs(dz) < EPSILON) ? Double.POSITIVE_INFINITY : zInvExit / dz;

		double entryTime = Math.max(xEntry, Math.max(yEntry, zEntry));
		double exitTime = Math.min(xExit, Math.min(yExit, zExit));

		if (entryTime > exitTime || exitTime < 0.0 || entryTime > 1.0) {
			return null;
		}
		Direction normal;
		double depth;

		if (entryTime < 0.0) {
			double overlapX = Math.min(movingMaxX, boxMaxX) - Math.max(movingMinX, voxelX);
			double overlapY = Math.min(movingMaxY, boxMaxY) - Math.max(movingMinY, voxelY);
			double overlapZ = Math.min(movingMaxZ, boxMaxZ) - Math.max(movingMinZ, voxelZ);
			if (overlapX < overlapY && overlapX < overlapZ) {
				depth = overlapX;
				normal = DIRECTIONS_X[FastMath.sign(dx) + 1];
			} else if (overlapY < overlapZ) {
				depth = overlapY;
				normal = DIRECTIONS_Y[FastMath.sign(dy) + 1];
			} else {
				depth = overlapZ;
				normal = DIRECTIONS_Z[FastMath.sign(dz) + 1];
			}
			entryTime = 0.0;
		} else {
			if (entryTime == xEntry) {
				normal = DIRECTIONS_X[FastMath.sign(dx) + 1];
			} else if (entryTime == yEntry) {
				normal = DIRECTIONS_Y[FastMath.sign(dy) + 1];
			} else {
				normal = DIRECTIONS_Z[FastMath.sign(dz) + 1];
			}
			depth = 0.0;
		}
		return new CollisionResult(entryTime, normal, depth);
	}

	public static void reload(MinecraftServer server) {
		applyConfig(server, BlockPhysicsConfig.load());
	}
}
