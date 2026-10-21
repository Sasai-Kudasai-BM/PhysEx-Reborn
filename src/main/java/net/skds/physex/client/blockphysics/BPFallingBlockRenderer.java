package net.skds.physex.client.blockphysics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.skds.physex.blockphysics.entity.BPFallingBlock;

public class BPFallingBlockRenderer extends EntityRenderer<BPFallingBlock, BPFallingBlockRenderer.RenderState> {

	public BPFallingBlockRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	@Override
	public void submit(RenderState entityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		BlockState blockState = entityRenderState.movingBlockRenderState.blockState;
		if (blockState.getRenderShape() == RenderShape.MODEL) {
			poseStack.pushPose();
			poseStack.translate(-0.5, 0.0, -0.5);
			submitNodeCollector.submitMovingBlock(poseStack, entityRenderState.movingBlockRenderState);
			poseStack.popPose();
			super.submit(entityRenderState, poseStack, submitNodeCollector, cameraRenderState);
		}
	}

	@Override
	public void extractRenderState(BPFallingBlock entity, RenderState entityRenderState, float f) {

		entityRenderState.x = Mth.lerp(f, entity.lastX, entity.currentX);
		entityRenderState.y = Mth.lerp(f, entity.lastY, entity.currentY);
		entityRenderState.z = Mth.lerp(f, entity.lastZ, entity.currentZ);
		entityRenderState.ageInTicks = entity.tickCount + f;
		entityRenderState.lightCoords = this.getPackedLightCoords(entity, f);
		//System.out.println(entity.xOld + " " + entity.getX());
		BlockPos blockPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
		entityRenderState.movingBlockRenderState.blockPos = blockPos;
		entityRenderState.movingBlockRenderState.blockState = entity.getBlockState();
		entityRenderState.movingBlockRenderState.biome = entity.level().getBiome(blockPos);
		entityRenderState.movingBlockRenderState.level = entity.level();
	}

	@Override
	public Vec3 getRenderOffset(RenderState entityRenderState) {
		return Vec3.ZERO;
	}

	@Override
	public RenderState createRenderState() {
		return new RenderState();
	}

	public static final class RenderState extends EntityRenderState {
		public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();

		RenderState() {
			this.eyeHeight = .5f;
			this.boundingBoxWidth = .5f;
			this.boundingBoxHeight = .5f;
			this.entityType = BPFallingBlock.TYPE;
		}
	}
}
