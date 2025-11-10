package net.skds.physex.fluids.layer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.skds.lib2.utils.ArrayUtils;

import static net.skds.physex.fluids.layer.FluidLayerConstants.ATTACHMENT_TYPE;


public record FluidLayer(SparseChunkStorage<FluidState> data) {

	public static final String DATA_KEY = "data";
	public static final String DICTIONARY_KEY = "dictionary";
	public static final ByteTag NULL_TAG = ByteTag.valueOf((byte) 0);

	FluidLayer() {
		this(new SparseChunkStorage<>(FluidState.class));
	}

	public static FluidState getFluidState(int x, int y, int z, ChunkAccess chunk) {
		FluidLayer layer = get(chunk);
		if (layer == null) return null;
		return layer.data.get(x, y - chunk.getMinY(), z);
	}

	public static FluidState getFluidState(BlockPos pos, ChunkAccess chunk) {
		FluidLayer layer = get(chunk);
		if (layer == null) return null;
		return layer.data.get(pos.getX(), pos.getY() - chunk.getMinY(), pos.getZ());
	}

	public static void setFluidState(BlockPos pos, ChunkAccess chunk, FluidState fs) {
		getOrCreate(chunk).data.set(pos.getX(), pos.getY() - chunk.getMinY(), pos.getZ(), fs);
	}

	public static void resetFluidState(BlockPos pos, ChunkAccess chunk) {
		FluidLayer layer = get(chunk);
		if (layer == null) return;
		layer.data.set(pos.getX(), pos.getY() - chunk.getMinY(), pos.getZ(), null);
	}

	public static FluidLayer get(ChunkAccess chunk) {
		return chunk.getAttached(ATTACHMENT_TYPE);
	}

	public static FluidLayer getOrCreate(ChunkAccess chunk) {
		return chunk.getAttachedOrCreate(ATTACHMENT_TYPE);
	}

	public static void update(ChunkAccess chunk) {
		FluidLayer old = chunk.setAttached(ATTACHMENT_TYPE, null);
		if (old != null) {
			chunk.setAttached(ATTACHMENT_TYPE, old);
		}
	}

	public void set(LevelChunk chunk) {
		chunk.setAttached(ATTACHMENT_TYPE, this);
	}

	static FluidLayer fromNbt(Tag tag) {
		try {
			CompoundTag nbt = tag.asCompound().orElse(null);
			if (nbt == null || nbt.isEmpty()) return new FluidLayer();
			return new FluidLayer(nbt);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return new FluidLayer();
		}
	}

	FluidLayer(CompoundTag nbt) {
		ListTag dictionary = nbt.getListOrEmpty(DICTIONARY_KEY);
		FluidState[] states = new FluidState[dictionary.size()];
		int i = 0;
		for (Tag tag : dictionary) {
			if (tag.getType() == ByteTag.TYPE) {
				states[i++] = null;
				continue;
			}
			FluidState fs = FluidState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
			states[i++] = fs;
		}
		long[] d = nbt.getLongArray(DATA_KEY).orElse(ArrayUtils.EMPTY_LONG);
		SparseChunkStorage.Data<FluidState> data = new SparseChunkStorage.Data<>(d, states);
		this(new SparseChunkStorage<>(data));
	}

	CompoundTag toNbt() {
		CompoundTag root = new CompoundTag();
		if (data.isEmpty()) return root;
		SparseChunkStorage.Data<FluidState> data = this.data().getData();
		root.put(DATA_KEY, new LongArrayTag(data.data()));

		ListTag dictionary = new ListTag();
		for (FluidState fs : data.dictionary()) {
			if (fs == null) {
				dictionary.add(NULL_TAG);
				continue;
			}
			Tag tag = FluidState.CODEC.encodeStart(NbtOps.INSTANCE, fs).getOrThrow();
			dictionary.add(tag);
		}

		root.put(DATA_KEY, new LongArrayTag(data.data()));
		root.put(DICTIONARY_KEY, dictionary);
		return root;
	}


	FluidLayer(FriendlyByteBuf buf) {
		long[] data = buf.readLongArray();
		int[] indexes = buf.readVarIntArray();
		FluidState[] states = new FluidState[indexes.length];
		for (int i = 0; i < indexes.length; i++) {
			int index = indexes[i];
			if (index == 0) {
				states[i] = null;
				continue;
			}
			states[i] = Fluid.FLUID_STATE_REGISTRY.byId(index - 1);
		}
		SparseChunkStorage.Data<FluidState> d = new SparseChunkStorage.Data<>(data, states);
		this(new SparseChunkStorage<>(d));
	}

	static void writeToBuffer(FriendlyByteBuf buf, FluidLayer layer) {
		SparseChunkStorage.Data<FluidState> d = layer.data().getData();
		FluidState[] states = d.dictionary();
		int[] indexes = new int[states.length];
		for (int i = 0; i < indexes.length; i++) {
			FluidState fs = states[i];
			if (fs == null) {
				indexes[i] = 0;
				continue;
			}
			indexes[i] = Fluid.FLUID_STATE_REGISTRY.getId(fs) + 1;
		}
		buf.writeLongArray(d.data());
		buf.writeVarIntArray(indexes);
	}

	public static boolean checkForSync(AttachmentTarget target, ServerPlayer player) {
		return true;
	}

	public static void init() {
		AttachmentRegistryImpl.register(ATTACHMENT_TYPE.identifier(), ATTACHMENT_TYPE);
	}
}
