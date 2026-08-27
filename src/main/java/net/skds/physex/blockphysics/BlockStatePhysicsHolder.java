package net.skds.physex.blockphysics;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public record BlockStatePhysicsHolder(
		BlockPhysicsData physicsData,
		BlockPhysicsData physicsDataNatural,
		Reference2ObjectOpenHashMap<ServerLevel, BlockPhysicsData> dimensions,
		Reference2ObjectOpenHashMap<ServerLevel, BlockPhysicsData> dimensionsNatural
) {

	public BlockStatePhysicsHolder(BlockPhysicsData data) {
		this(data, null, null, null);
	}

	public BlockPhysicsData getPhysicsData(ServerLevel dimension, boolean natural) {
		Reference2ObjectOpenHashMap<ServerLevel, BlockPhysicsData> map;
		BlockPhysicsData defaultData;
		if (natural) {
			defaultData = this.physicsDataNatural;
			map = this.dimensionsNatural;
			if (map == null) {
				map = this.dimensions;
			}
			if (defaultData == null) {
				defaultData = this.physicsData;
			}
		} else {
			defaultData = this.physicsData;
			map = this.dimensions;
		}
		if (map != null) {
			BlockPhysicsData mapValue = map.get(dimension);
			if (mapValue != null) {
				return mapValue;
			}
		}
		return defaultData;
	}

	public String toStringFormatted() {
		StringBuilder sb = new StringBuilder("======================")
				.append('\n').append(physicsData.toStringFormatted())
				.append("\nNatural:").append(physicsDataNatural != null ? physicsDataNatural.toStringFormatted() : "null")
				.append("\nDimensions:\n");
		if (dimensions != null) for (var e : dimensions.entrySet()) {
			sb.append(e.getKey()).append(": ").append(e.getValue().toStringFormatted()).append('\n');
		}
		sb.append("Natural dimensions:\n");
		if (dimensionsNatural != null) for (var e : dimensionsNatural.entrySet()) {
			sb.append(e.getKey().dimension().identifier()).append(": ").append(e.getValue().toStringFormatted()).append('\n');
		}
		return sb.toString();
	}

	public static BlockPhysicsData get(BlockState state, ServerLevel dimension, boolean natural) {
		return ((BlockStatePhysicsAccessor) state).physEx$getHolder().getPhysicsData(dimension, natural);
	}

	public static BlockStatePhysicsHolder get(BlockState state) {
		return ((BlockStatePhysicsAccessor) state).physEx$getHolder();
	}
}
