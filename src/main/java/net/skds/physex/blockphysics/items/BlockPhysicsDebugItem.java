package net.skds.physex.blockphysics.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.BlockStatePhysicsHolder;

public class BlockPhysicsDebugItem extends Item {

	public BlockPhysicsDebugItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext useOnContext) {

		if (!useOnContext.getLevel().isClientSide() && useOnContext.getPlayer() != null) {
			BlockPos blockPos = useOnContext.getClickedPos();
			BlockStatePhysicsHolder physicsData = BlockStatePhysicsHolder.get(useOnContext.getLevel().getBlockState(blockPos));
			useOnContext.getPlayer().displayClientMessage(Component.literal(physicsData.toStringFormatted()), false);
		}

		return InteractionResult.SUCCESS;
	}

	public static void reg() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, "debug"));
		Registry.register(
				BuiltInRegistries.ITEM,
				key.identifier(),
				new BlockPhysicsDebugItem(new Properties().setId(key))
		);
	}
}
