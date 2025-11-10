package net.skds.physex.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Iterator;

public class FluidStorageItemComponent implements Storage<FluidVariant> {
	@Override
	public boolean supportsInsertion() {
		return Storage.super.supportsInsertion();
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public boolean supportsExtraction() {
		return Storage.super.supportsExtraction();
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public Iterator<StorageView<FluidVariant>> iterator() {
		return null;
	}
}
