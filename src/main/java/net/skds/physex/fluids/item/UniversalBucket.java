package net.skds.physex.fluids.item;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.skds.physex.fluids.FluidUtils;
import net.skds.physex.fluids.PhysExFluidItems;

public class UniversalBucket extends Item {

	public UniversalBucket(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public boolean isBarVisible(ItemStack itemStack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack itemStack) {
		FluidLevelsStorage content = itemStack.getComponents().getOrDefault(PhysExFluidItems.FLUID_HOLDER_COMPONENT, FluidLevelsStorage.EMPTY);
		int levels = content.levels();
		return Mth.clamp(Math.round(levels * 13.0f / FluidUtils.MAX_LEVEL), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack itemStack) {
		return 0x4040E0;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
		BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (blockHitResult.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		ItemStack itemStack = player.getItemInHand(interactionHand);
		BlockPos blockPos = blockHitResult.getBlockPos();
		Direction direction = blockHitResult.getDirection();
		BlockPos blockPos2 = blockPos.relative(direction);
		if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos2, direction, itemStack)) {
			return InteractionResult.FAIL;
		}
		FluidState targetFs = level.getFluidState(blockPos);
		boolean pick = player.isCrouching() && !targetFs.isEmpty();
		FluidLevelsStorage content = itemStack.getOrDefault(PhysExFluidItems.FLUID_HOLDER_COMPONENT, FluidLevelsStorage.EMPTY);
		if (pick) {
			FluidLevelsStorage picked = FluidUtils.pickUpFluid(level, content.fluid().getFluid(), blockPos, FluidUtils.MAX_LEVEL - content.levels());
			if (!picked.isEmpty()) {
				player.awardStat(Stats.ITEM_USED.get(this));
				SoundEvent se = FluidVariantAttributes.getFillSound(picked.fluid());
				player.playSound(se, 1.0f, 1.0f);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
				content = new FluidLevelsStorage(content.fluid(), content.levels() + picked.levels());
				@SuppressWarnings("DataFlowIssue")
				ItemStack itemStack2 = PhysExFluidItems.UNIVERSAL_BUCKET.createStack(content);
				ItemStack itemStack3 = ItemUtils.createFilledResult(itemStack, player, itemStack2);
				return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack3);
			}
		} else {
			SoundEvent se = FluidVariantAttributes.getEmptySound(content.fluid());
			player.playSound(se, 1.0f, 1.0f);
			if (!level.isClientSide() && content.fluid().getFluid() instanceof FlowingFluid ff) {
				int rem = FluidUtils.placeFluid((ServerLevel) level, blockPos2, ff, content.levels());
				if (rem != content.levels()) {
					if (rem == 0) {
						player.awardStat(Stats.ITEM_USED.get(this));
						ItemStack itemStack2 = ItemUtils.createFilledResult(itemStack, player, BucketItem.getEmptySuccessItem(itemStack, player));
						return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack2);
					}
					player.awardStat(Stats.ITEM_USED.get(this));
					ItemStack newBucket = createStack(new FluidLevelsStorage(content.fluid(), rem));
					ItemStack itemStack2 = ItemUtils.createFilledResult(itemStack, player, newBucket);
					return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack2);
				}
			}
		}
		return InteractionResult.FAIL;
	}

	public ItemStack createStack(FluidLevelsStorage storage) {
		Item bucket = storage.fluid().getFluid().getBucket();
		if (storage.levels() == FluidUtils.MAX_LEVEL) return new ItemStack(bucket);
		ItemStack stack = new ItemStack(this, 1);
		stack.set(PhysExFluidItems.FLUID_HOLDER_COMPONENT, storage);
		stack.set(DataComponents.ITEM_NAME, Component.translatable(bucket.getDescriptionId()));
		stack.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(bucket));
		return stack;
	}
}
