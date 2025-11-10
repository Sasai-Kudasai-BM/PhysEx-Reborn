package net.skds.physex.fluids.layer;

import com.mojang.serialization.Codec;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentTypeImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.skds.physex.PhysEx;

@UtilityClass
public class FluidLayerConstants {

	static final StreamCodec<FriendlyByteBuf, FluidLayer> STREAM_CODEC = StreamCodec.of(FluidLayer::writeToBuffer, FluidLayer::new);
	public static final Codec<FluidLayer> CODEC = ExtraCodecs.NBT.xmap(FluidLayer::fromNbt, FluidLayer::toNbt);

	public static final AttachmentType<FluidLayer> ATTACHMENT_TYPE = new AttachmentTypeImpl<>(
			ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, "fluid_layer"),
			FluidLayer::new,
			CODEC,
			STREAM_CODEC,
			FluidLayer::checkForSync,
			false
	);
}
