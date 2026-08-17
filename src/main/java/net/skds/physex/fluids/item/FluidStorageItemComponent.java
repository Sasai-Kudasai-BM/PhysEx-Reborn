package net.skds.physex.fluids.item;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Items;
import net.skds.physex.fluids.FluidUtils;
import net.skds.physex.fluids.PhysExFluidItems;

public class FluidStorageItemComponent implements
		Storage<FluidVariant>, SingleSlotStorage<FluidVariant> {

	private static final DataComponentType<FluidLevelsStorage> COMPONENT_TYPE = PhysExFluidItems.FLUID_HOLDER_COMPONENT;

	private final ContainerItemContext context;

	public FluidStorageItemComponent(ContainerItemContext context) {
		this.context = context;
	}

	private FluidLevelsStorage getStorage() {
		ItemVariant variant = context.getItemVariant();
		if (variant.isBlank()) {
			return FluidLevelsStorage.EMPTY;
		}
		return variant.getComponentMap().getOrDefault(COMPONENT_TYPE, FluidLevelsStorage.EMPTY);
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		if (resource.isBlank()) return 0;
		FluidLevelsStorage storage = getStorage();
		if (!storage.fluid().equals(resource)) {
			return 0;
		}
		int requested = (int) (maxAmount / FluidUtils.FLUID_IN_LEVEL);
		int contains = storage.levels();
		int toTransfer = Math.min(requested, FluidUtils.MAX_LEVEL - contains);
		if (toTransfer != 0) {
			contains += toTransfer;
			ItemVariant newVariant;
			if (contains != FluidUtils.MAX_LEVEL) {
				storage = new FluidLevelsStorage(storage.fluid(), contains);
				DataComponentPatch patch = DataComponentPatch.builder().set(COMPONENT_TYPE, storage).build();
				newVariant = context.getItemVariant().withComponentChanges(patch);
			} else {
				newVariant = ItemVariant.of(storage.fluid().getFluid().getBucket());
			}
			if (context.exchange(newVariant, 1, transaction) == 1) {
				return (long) toTransfer * FluidUtils.FLUID_IN_LEVEL;
			}
		}
		return 0;
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		if (resource.isBlank()) return 0;
		FluidLevelsStorage storage = getStorage();
		if (!storage.fluid().equals(resource)) {
			return 0;
		}
		int requested = (int) (maxAmount / FluidUtils.FLUID_IN_LEVEL);
		int contains = storage.levels();
		int toTransfer = Math.min(requested, contains);
		if (toTransfer != 0) {
			contains -= toTransfer;
			ItemVariant newVariant;
			if (contains != 0) {
				storage = new FluidLevelsStorage(storage.fluid(), contains);
				DataComponentPatch patch = DataComponentPatch.builder().set(COMPONENT_TYPE, storage).build();
				newVariant = context.getItemVariant().withComponentChanges(patch);
			} else {
				newVariant = ItemVariant.of(Items.BUCKET);
			}
			if (context.exchange(newVariant, 1, transaction) == 1) {
				return (long) toTransfer * FluidUtils.FLUID_IN_LEVEL;
			}
		}
		return 0;
	}

	@Override
	public boolean isResourceBlank() {
		return getStorage().isEmpty();
	}

	@Override
	public FluidVariant getResource() {
		return getStorage().fluid();
	}

	@Override
	public long getAmount() {
		return (long) getStorage().levels() * FluidUtils.FLUID_IN_LEVEL;
	}

	@Override
	public long getCapacity() {
		return FluidConstants.BUCKET;
	}
}
