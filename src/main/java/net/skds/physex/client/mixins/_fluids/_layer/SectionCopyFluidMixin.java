package net.skds.physex.client.mixins._fluids._layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.skds.physex.client.fluids.layer.SectionCopyExtension;
import net.skds.physex.fluids.layer.FluidLayer;
import net.skds.physex.fluids.layer.SparseChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionCopy.class)
public class SectionCopyFluidMixin implements SectionCopyExtension {

	@Mutable
	@Unique
	private Int2ObjectOpenHashMap<FluidState> fluids;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(LevelChunk levelChunk, int i, CallbackInfo ci) {
		FluidLayer fl = FluidLayer.get(levelChunk);
		if (fl != null) {
			this.fluids = fl.data().cutSection(i * 16 - levelChunk.getMinY());
			return;
		}
		this.fluids = null;
	}

	@Unique
	@Override
	public FluidState physEx$getFluidState(int x, int y, int z) {
		if (fluids == null) return Fluids.EMPTY.defaultFluidState();
		return fluids.getOrDefault(SparseChunkStorage.getPosIndex(x, y, z), Fluids.EMPTY.defaultFluidState());
	}
}
