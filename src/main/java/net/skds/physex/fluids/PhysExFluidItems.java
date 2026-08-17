package net.skds.physex.fluids;

import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.skds.physex.PhysEx;
import net.skds.physex.fluids.item.FluidLevelsStorage;
import net.skds.physex.fluids.item.UniversalBucket;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class PhysExFluidItems {

	public static final DataComponentType<FluidLevelsStorage> FLUID_HOLDER_COMPONENT = registerComponent(
			"stored_fluid",
			b -> b.networkSynchronized(FluidLevelsStorage.STREAM_CODEC)
					.persistent(FluidLevelsStorage.CODEC)
	);

	public static final UniversalBucket UNIVERSAL_BUCKET = registerItem(
			"universal_bucket",
			UniversalBucket::new
	);

	private static <T> DataComponentType<T> registerComponent(String id, UnaryOperator<DataComponentType.Builder<T>> builder) {
		if (!PhysEx.fluidItemsEnabled()) return null;
		return Registry.register(
				BuiltInRegistries.DATA_COMPONENT_TYPE,
				ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, id),
				builder.apply(DataComponentType.builder()).build()
		);
	}

	private static <T extends Item> T registerItem(String id, Function<Item.Properties, T> function) {
		if (!PhysEx.fluidItemsEnabled()) return null;
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(PhysEx.MOD_ID, id));
		Item.Properties properties = new Item.Properties();
		properties.setId(key);
		return Registry.register(
				BuiltInRegistries.ITEM,
				key.location(),
				function.apply(properties)
		);
	}

	public static void init() {
		//FluidStorage.ITEM.registerForItems(
		//		(itemStack, context) -> new FluidStorageItemComponent(context),
		//		UNIVERSAL_BUCKET
		//);
		//noinspection DataFlowIssue
		ComponentTooltipAppenderRegistry.addFirst(FLUID_HOLDER_COMPONENT);
	}
}
