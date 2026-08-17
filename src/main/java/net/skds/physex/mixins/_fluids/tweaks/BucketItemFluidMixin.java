package net.skds.physex.mixins._fluids.tweaks;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.skds.physex.PhysEx;
import net.skds.physex.fluids.FluidDistribution;
import net.skds.physex.fluids.FluidUtils;
import net.skds.physex.fluids.PhysExFluidItems;
import net.skds.physex.fluids.item.FluidLevelsStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemFluidMixin extends Item {

	@Shadow
	@Final
	private Fluid content;

	private BucketItemFluidMixin(Properties properties) {
		super(properties);
	}

	@Shadow
	public abstract boolean emptyContents(@Nullable LivingEntity livingEntity, Level level, BlockPos blockPos, @Nullable BlockHitResult blockHitResult);

	@Shadow
	protected abstract void playEmptySound(@Nullable LivingEntity livingEntity, LevelAccessor levelAccessor, BlockPos blockPos);

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	public void use(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!PhysEx.fluidItemsEnabled()) return;
		if (this.content == Fluids.EMPTY) {
			BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
			if (blockHitResult.getType() != HitResult.Type.BLOCK) {
				cir.setReturnValue(InteractionResult.PASS);
				return;
			}
			ItemStack itemStack = player.getItemInHand(interactionHand);
			BlockPos blockPos = blockHitResult.getBlockPos();
			Direction direction = blockHitResult.getDirection();
			BlockPos blockPos2 = blockPos.relative(direction);
			if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos2, direction, itemStack)) {
				cir.setReturnValue(InteractionResult.FAIL);
				return;
			}
			FluidLevelsStorage picked = FluidUtils.pickUpFluid(level, null, blockPos, FluidUtils.MAX_LEVEL);
			if (!picked.isEmpty()) {
				player.awardStat(Stats.ITEM_USED.get(this));
				SoundEvent se = picked.fluid().getFluid().getPickupSound().orElse(null);
				if (se != null) {
					player.playSound(se, 1.0f, 1.0f);
				}
				level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
				@SuppressWarnings("DataFlowIssue")
				ItemStack itemStack2 = PhysExFluidItems.UNIVERSAL_BUCKET.createStack(picked);
				ItemStack itemStack3 = ItemUtils.createFilledResult(itemStack, player, itemStack2);
				if (!level.isClientSide()) {
					CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, itemStack2);
				}

				cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(itemStack3));
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
	void emptyContents(LivingEntity livingEntity, Level level, BlockPos blockPos, BlockHitResult blockHitResult, CallbackInfoReturnable<Boolean> cir) {
		if (!(this.content instanceof FlowingFluid flowingFluid)) {
			cir.setReturnValue(false);
		} else {
			BlockState blockState = level.getBlockState(blockPos);
			Block block = blockState.getBlock();
			boolean bl = blockState.canBeReplaced(this.content);
			boolean bl2 = livingEntity != null && livingEntity.isShiftKeyDown();
			boolean bl3 = bl
					|| block instanceof LiquidBlockContainer liquidBlockContainer
					&& liquidBlockContainer.canPlaceLiquid(livingEntity, level, blockPos, blockState, this.content);
			boolean bl4 = blockState.isAir() || bl3 && (!bl2 || blockHitResult == null);
			if (!bl4) {
				boolean ret = blockHitResult != null && this.emptyContents(livingEntity,
						level,
						blockHitResult.getBlockPos().relative(blockHitResult.getDirection()),
						null
				);
				cir.setReturnValue(ret);
			} else if (level.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
				int i = blockPos.getX();
				int j = blockPos.getY();
				int k = blockPos.getZ();
				level.playSound(
						livingEntity,
						blockPos,
						SoundEvents.FIRE_EXTINGUISH,
						SoundSource.BLOCKS,
						0.5F,
						2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
				);
				for (int l = 0; l < 8; l++) {
					level.addParticle(ParticleTypes.LARGE_SMOKE, i + Math.random(), j + Math.random(), k + Math.random(), 0.0, 0.0, 0.0);
				}
				cir.setReturnValue(true);
			} else if (!level.isClientSide()) {
				FluidDistribution space = FluidUtils.findSpaceForFluid((ServerLevel) level, blockPos, flowingFluid, FluidUtils.MAX_LEVEL, FluidUtils.FS_GETTER);
				if (space.remainingFluid() > 0) {
					cir.setReturnValue(false);
					return;
				}
				FluidUtils.placeFluid((ServerLevel) level, flowingFluid, space);

				this.playEmptySound(livingEntity, level, blockPos);
				cir.setReturnValue(true);
			}
		}
	}
}
