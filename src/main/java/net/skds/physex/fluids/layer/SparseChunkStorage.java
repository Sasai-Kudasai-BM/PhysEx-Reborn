package net.skds.physex.fluids.layer;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import net.skds.lib2.utils.ArrayUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.ToIntFunction;

public class SparseChunkStorage<T> {

	private static final int INITIAL_SIZE = 16;

	@Getter
	private T[] states;
	@Getter
	private int[] stateCount;

	private final Int2IntOpenHashMap data = new Int2IntOpenHashMap();
	private final Object2IntOpenHashMap<T> dictionary = new Object2IntOpenHashMap<>();
	private final BitSet slots = new BitSet();
	private final ToIntFunction<T> freeSeeker = fs -> {
		int index = slots.nextClearBit(0);
		slots.set(index);
		ensureCapacity(index);
		states[index] = fs;
		stateCount[index] = 0;
		return index;
	};

	public SparseChunkStorage(Class<T> type) {
		this.states = ArrayUtils.createGenericArray(type, INITIAL_SIZE);
		this.stateCount = new int[INITIAL_SIZE];
	}

	public SparseChunkStorage(Data<T> data) {
		T[] dictionary = data.dictionary;
		this.states = dictionary.clone();
		this.stateCount = new int[dictionary.length];
		for (int i = 0; i < dictionary.length; i++) {
			T t = dictionary[i];
			if (t != null) {
				this.slots.set(i);
				this.dictionary.put(t, i);
			}
		}
		long[] d = data.data;
		for (long l : d) {
			int pos = (int) (l >>> 32);
			int value = (int) (l & 0xFFFFFF);
			this.data.put(pos, value + 1);
			this.stateCount[value]++;
		}
		//for (int i = 0; i < dictionary.length; i++) {
		//	if (stateCount[i] == 0) {
		//		removeIndex(i);
		//	}
		//}
	}

	public boolean isEmpty() {
		return dictionary.isEmpty();
	}

	public T get(int x, int y, int z) {
		int pos = getPosIndex(x, y, z);
		int index = data.get(pos) - 1;
		if (index >= 0) {
			return states[index];
		}
		return null;
	}

	public void set(int x, int y, int z, T state) {
		int pos = getPosIndex(x, y, z);

		if (state != null) {
			int index = dictionary.computeIfAbsent(state, freeSeeker);
			int prev = data.put(pos, index + 1) - 1;
			if (prev >= 0) {
				if (prev != index) {
					stateCount[index]++;
					int c = --stateCount[prev];
					if (c == 0) {
						removeIndex(prev);
					}
				}
			} else {
				stateCount[index]++;
			}
		} else {
			int prev = data.remove(pos) - 1;
			if (prev >= 0) {
				int c = --stateCount[prev];
				if (c == 0) {
					removeIndex(prev);
				}
			}
		}
	}

	private void removeIndex(int index) {
		T remove = states[index];
		states[index] = null;
		dictionary.removeInt(remove);
		slots.clear(index);
	}

	private void ensureCapacity(int index) {
		int l = this.states.length;
		if (l <= index) {
			if (l < INITIAL_SIZE / 2) {
				l = INITIAL_SIZE;
			} else {
				l *= 2;
			}
			this.states = Arrays.copyOf(this.states, l);
			this.stateCount = Arrays.copyOf(this.stateCount, l);
		}
	}

	public Data<T> getData() {
		long[] data = new long[this.data.size()];
		int i = 0;
		for (var e : this.data.int2IntEntrySet()) {
			data[i++] = ((long) e.getIntKey() << 32) | (e.getIntValue() - 1);
		}

		return new Data<>(data, Arrays.copyOf(this.states, slots.length()));
	}

	public Int2ObjectOpenHashMap<T> cutSection(int bottomY) {
		if (isEmpty()) return null;
		int bottomIndex = bottomY << 8;
		Int2ObjectOpenHashMap<T> map = new Int2ObjectOpenHashMap<>();
		for (var e : this.data.int2IntEntrySet()) {
			int ei = e.getIntKey() - bottomIndex;
			if (ei >= 0 && ei < 16 << 8) {
				int word = e.getIntValue() - 1;
				if (word >= 0) {
					T value = states[word];
					if (value != null) {
						map.put(ei, value);
					}
				}
			}
		}
		return map.isEmpty() ? null : map;
	}

	public record Data<T>(long[] data, T[] dictionary) {
	}

	public static int getPosIndex(int x, int y, int z) {
		return (y << 4 | (x & 0xf)) << 4 | (z & 0xf);
	}
}
