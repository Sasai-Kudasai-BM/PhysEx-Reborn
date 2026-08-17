package net.skds.physex.fluids.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.material.Fluid;
import net.skds.physex.PhysEx;
import net.skds.physex.fluids.FluidUtils;

import java.util.function.Consumer;

public record FluidLevelsStorage(FluidVariant fluid, int levels) implements TooltipProvider {

	public FluidLevelsStorage(Fluid fluid, int levels) {
		this(FluidVariant.of(fluid), levels);
	}

	public static final FluidLevelsStorage EMPTY = new FluidLevelsStorage(FluidVariant.blank(), 0);

	public static final Codec<FluidLevelsStorage> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					FluidVariant.CODEC.fieldOf("fluid").forGetter(FluidLevelsStorage::fluid),
					Codec.INT.fieldOf("levels").forGetter(FluidLevelsStorage::levels)
			).apply(instance, FluidLevelsStorage::new)
	);

	public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FluidLevelsStorage> STREAM_CODEC = StreamCodec.composite(
			FluidVariant.PACKET_CODEC, FluidLevelsStorage::fluid,
			ByteBufCodecs.VAR_INT, FluidLevelsStorage::levels,
			FluidLevelsStorage::new
	);

	public boolean isEmpty() {
		return this == EMPTY || levels == 0 || fluid.isBlank();
	}

	@Override
	public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
		if (!this.isEmpty()) {
			Component fluidName = FluidVariantAttributes.getName(this.fluid);
			String amount = "%.02f".formatted((float) this.levels / FluidUtils.MAX_LEVEL);
			consumer.accept(Component.translatable("tooltip." + PhysEx.MOD_ID + ".bucket_content", fluidName, amount));
		}
	}
}
