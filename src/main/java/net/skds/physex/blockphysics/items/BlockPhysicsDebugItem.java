package net.skds.physex.blockphysics.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.skds.physex.PhysEx;
import net.skds.physex.blockphysics.BlockPhysicsManager;
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

	@Override
	public boolean canDestroyBlock(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, LivingEntity livingEntity) {
		if (!level.isClientSide()) {
			BlockPhysicsManager.get((ServerLevel) level).scheduleBlockCheck(blockPos);
		}
		return false;
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
