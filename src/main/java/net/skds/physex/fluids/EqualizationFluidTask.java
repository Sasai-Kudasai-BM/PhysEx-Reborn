package net.skds.physex.fluids;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.FlowingFluid;
import net.skds.lib2.mat.FastMath;
import net.skds.lib2.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Comparator;

public class EqualizationFluidTask extends AbstractFluidTask {

	private static final Comparator<Object2IntMap.Entry<BlockPos>> COMP =
			(e1, e2) -> Integer.compare(e2.getIntValue(), e1.getIntValue());

	public EqualizationFluidTask(BlockPos pos, FlowingFluid fluid, ServerLevel world) {
		super(pos, fluid, world);
	}

	@Override
	public void run() {
		if (isThis(getFluidState(pos))) {
			equalize();
		}
	}

	private void equalize() {
		int amount = getFluidQuantity(pos);
		int n = 1;
		int nl = 0;
		int min = MAX_LEVEL;
		int max = -MAX_LEVEL;

		int y = pos.getY();

		int steps = FastMath.clamp(FluidUtils.getSlopeDistance(fluid, world) * 4, 2, 20);

		Object2IntOpenHashMap<BlockPos> fluids = new Object2IntOpenHashMap<>(32, 0.5f);
		ObjectOpenHashSet<BlockPos> newPoses = new ObjectOpenHashSet<>(32, 0.5f);
		ObjectOpenHashSet<BlockPos> nextNewPoses = new ObjectOpenHashSet<>(32, 0.5f);
		newPoses.add(pos);
		fluids.put(pos, amount);

		l1:
		for (int i = 0; i < steps; i++) {
			for (BlockPos p : newPoses) {
				int py = p.getY();
				boolean walls = false;
				boolean hitEmpty = false;
				for (Direction dir : randDirs) {
					BlockPos p2 = p.relative(dir);
					if (!fluids.containsKey(p2)) {
						if (havePath(p, dir, true)) {
							int q = getFluidQuantity(p2);
							amount += q;
							if (py < y) {
								//if (!havePath(p, dir)) {
								//	int qm = q - MAX_LEVEL;
								//	if (qm < min) min = qm;
								//}
								nl++;
							} else {
								n++;
								if (q > max) max = q;
								if (q < min) min = q;
							}
							fluids.put(p2, q);
							if (q != 0) {
								if (fluidTicks.allowedForEqualization(p2)) nextNewPoses.add(p2);
								//if (havePath(p2, Direction.DOWN)) {
								//	if (getFluidQuantity(p2.below()) == MAX_LEVEL) {
								//		nextNewPoses.add(p2);
								//	}
								//} else if (i < 2) {
								//	nextNewPoses.add(p2);
								//}
							} else {
								walls = true;
								hitEmpty = true;
								if (0 < min) min = 0;
							}
						} else walls = true;
					}
				}
				if (walls && py == y) {
					BlockPos p2 = p.below();
					if (!fluids.containsKey(p2)) {
						if (havePath(p, Direction.DOWN, true)) {
							int q = getFluidQuantity(p2);
							amount += q;
							if (hitEmpty) {
								int qmm = q - MAX_LEVEL;
								if (qmm < min) min = qmm;
							}
							fluids.put(p2, q);
							if (q != 0) {
								if (fluidTicks.allowedForEqualization(p2)) nextNewPoses.add(p2);
							}
							nl++;
						}
					}
				}
				int nPlus = n + nl;
				if (nPlus > steps * 4 && amount % nPlus == 0)
					break l1;
				//if ((max - min > 1) && py == y)
				//	break l1;
			}
			if (nextNewPoses.isEmpty())
				break;
			var tmp = newPoses;
			newPoses = nextNewPoses;
			nextNewPoses = tmp;
			nextNewPoses.clear();
		}
		if (max - min < 2 || amount / n < 1) {
			//System.out.println("flat");
			for (BlockPos bp : fluids.keySet()) {
				fluidTicks.done(bp);
			}
			return;
		}
		equalizeFill(fluids, amount, n, nl, y);
	}

	@SuppressWarnings("unchecked")
	private void equalizeFill(Object2IntOpenHashMap<BlockPos> fluids, int amount, int n, int nl, int y) {
		int capL = MAX_LEVEL * nl;

		boolean bottom = amount <= capL;
		int mid;
		int rem;
		if (bottom) {
			mid = amount / nl;
			rem = amount % nl;
		} else {
			amount -= capL;
			mid = amount / n;
			rem = amount % n;
		}
		Object2IntMap.Entry<BlockPos>[] entriesL = ArrayUtils.createGenericArray(Object2IntMap.Entry.class, fluids.size());
		Object2IntMap.Entry<BlockPos>[] entries = ArrayUtils.createGenericArray(Object2IntMap.Entry.class, fluids.size());
		int i = 0;
		int il = 0;
		for (var e : fluids.object2IntEntrySet()) {
			if (e.getKey().getY() == y) {
				entries[i++] = e;
			} else {
				entriesL[il++] = e;
			}
		}
		entries = Arrays.copyOf(entries, i);
		entriesL = Arrays.copyOf(entriesL, il);
		if (rem > 0) {
			Arrays.sort(entriesL, COMP);
		}

		for (var e : entriesL) {
			BlockPos p = e.getKey();
			int l;
			if (bottom) {
				l = mid;
				if (rem > 0) {
					rem--;
					l++;
				}
			} else {
				l = MAX_LEVEL;
			}
			if (l != e.getIntValue()) {
				setFluid(p, l);
			}
			fluidTicks.done(p);
		}

		for (var e : entries) {
			BlockPos p = e.getKey();
			int l;
			if (bottom) {
				l = 0;
			} else {
				l = mid;
				if (rem > 0) {
					rem--;
					l++;
				}
			}
			if (l != e.getIntValue()) {
				setFluid(p, l);
			}
			fluidTicks.done(p);
		}
		//System.out.println(amount + " / " + n + " " + mid + ":" + rem);
	}
}
