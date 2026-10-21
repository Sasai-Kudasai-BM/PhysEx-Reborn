package net.skds.physex.blockphysics.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skds.physex.PhysEx;
import net.skds.physex.PhysExBootConfig;
import net.skds.physex.blockphysics.BlockPhysicsData;
import net.skds.physex.blockphysics.BlockPhysicsUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class BPFallingBlock extends Entity implements BlockPhysicsUtils.WorldCollisionChecker {

	public static final double RELATIVISTIC_SPEED = 16;
	public static final double RELATIVISTIC_SPEED_SQR = RELATIVISTIC_SPEED * RELATIVISTIC_SPEED;
	public static final double RELATIVISTIC_SPEED_THRESHOLD = RELATIVISTIC_SPEED_SQR / 4;

	public static final String TAG_BLOCK_STATE = "block_state";
	public static final String TAG_MASS = "mass";

	public static final int UPDATE_RATE = 1;
	public static final EntityType<BPFallingBlock> TYPE = register(
			EntityType.Builder.of(BPFallingBlock::new, MobCategory.MISC)
					.sized(1, 1)
					.eyeHeight(.5f)
					.clientTrackingRange(8)
					.updateInterval(UPDATE_RATE)
					.noLootTable()
	);

	@Getter
	private BlockState blockState;
	private int stateId;

	public double lastX, lastY, lastZ;
	public double currentX, currentY, currentZ;

	private double mass;
	private double effectiveMass;
	private Vec3 relV = Vec3.ZERO;

	private final BlockPos.MutableBlockPos cursor1 = new BlockPos.MutableBlockPos();

	public BPFallingBlock(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	public BPFallingBlock(Level level, BlockState state, BlockPos pos, BlockPhysicsData physicsData) {
		super(TYPE, level);
		Objects.requireNonNull(state);
		setBlockState(state);
		setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
	}

	@Override
	protected double getDefaultGravity() {
		return 10d / 400;
	}

	@Override
	public void baseTick() {
		ProfilerFiller profilerFiller = Profiler.get();
		profilerFiller.push("entityBaseTick");
		this.computeSpeed();
		this.checkBelowWorld();
		profilerFiller.pop();
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			this.lastX = this.currentX;
			this.lastY = this.currentY;
			this.lastZ = this.currentZ;
			this.currentX = getX();
			this.currentY = getY();
			this.currentZ = getZ();
		} else {
			super.tick();

			moveTick();
			//System.out.println("=========");

		}
	}

	private void moveTick() {
		this.applyGravity();
		Vec3 v = this.getDeltaMovement();
		Vec3 move = this.relV;
		var cr = BlockPhysicsUtils.traverseAABB(getX(), getY() + .5, getZ(),
				move.x, move.y, move.z,
				.5,
				this
		);
		if (cr == null) {
			doMove(move);
		} else {
			// TODO indirect

			if (cr.tHit() == 0 || cr.normal() == null || (cr.normal() == Direction.DOWN && v.lengthSqr() < 0.05 && v.y < 0)) {
				discard();
				return;
			}

			doMove(move.scale(cr.tHit()));
			Vec3 nVec = cr.normal().getUnitVec3();

			double vNorm = -nVec.dot(v);
			double vBounce = vNorm * getBounce((float) v.lengthSqr());
			Vec3 tan = v.subtract(nVec.scale(-vNorm));
			double vTan = tan.length();
			double vFriction = Math.max(0, vTan - vNorm * getFriction());
			tan = tan.scale(vFriction);
			Vec3 v2 = tan.add(nVec.scale(vBounce));
			setDeltaMovement(v2);
			if (vNorm > 0.5) {
				SoundType sound = blockState.getSoundType();
				level().playSound(null, this, sound.getFallSound(), SoundSource.MASTER, sound.volume * Math.min(2, (float) vNorm), sound.pitch);
			}
		}
	}

	@Override
	public boolean isSolid(int x, int y, int z) {
		cursor1.set(x, y, z);
		Level w = level();
		BlockState state = w.getBlockState(cursor1);
		//BlockPhysicsDebug.debug(cursor1, Blocks.GLASS.defaultBlockState());
		if (state.isAir()) {
			return false;
		}
		VoxelShape shape = state.getCollisionShape(w, cursor1);
		return !shape.isEmpty();
	}

	@Override
	public void setDeltaMovement(Vec3 velocity) {
		super.setDeltaMovement(velocity);
		double vSqr = velocity.lengthSqr();
		if (vSqr > RELATIVISTIC_SPEED_THRESHOLD) {
			double gamma = Math.sqrt(1.0 + (vSqr / RELATIVISTIC_SPEED_SQR));
			this.effectiveMass = this.mass * gamma;
			this.relV = velocity.scale(1.0 / gamma);
		} else {
			this.relV = velocity;
			this.effectiveMass = this.mass;
		}
	}

	public void doMove(Vec3 move) {
		setPos(new Vec3(
				this.getX() + move.x,
				this.getY() + move.y,
				this.getZ() + move.z
		));
	}

	private float getBounce(float vSqr) {
		return vSqr > 0.01F ? 0.5F : 0;
	}

	private float getFriction() {
		return 0.5F;
	}

	@Override
	public boolean canCollideWith(Entity other) {
		return false;
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		return true;
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket clientboundAddEntityPacket) {
		super.recreateFromPacket(clientboundAddEntityPacket);
		this.blockState = Block.stateById(clientboundAddEntityPacket.getData());
		double x = clientboundAddEntityPacket.getX();
		double y = clientboundAddEntityPacket.getY();
		double z = clientboundAddEntityPacket.getZ();
		super.setPos(x, y, z);
		this.lastX = x;
		this.lastY = y;
		this.lastZ = z;
		this.currentX = x;
		this.currentY = y;
		this.currentZ = z;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		this.setBlockState(valueInput.read(TAG_BLOCK_STATE, BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));
		this.mass = valueInput.getDoubleOr(TAG_MASS, 1000);
		setDeltaMovement(valueInput.read("Motion", Vec3.CODEC).orElse(Vec3.ZERO));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		valueOutput.store(TAG_BLOCK_STATE, BlockState.CODEC, this.blockState);
		valueOutput.putDouble(TAG_MASS, this.mass);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {

	}

	public final void setBlockState(BlockState blockState) {
		this.blockState = blockState;
		this.stateId = Block.getId(blockState);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
		return new ClientboundAddEntityPacket(
				this.getId(),
				this.getUUID(),
				this.getX(),
				this.getY(),
				this.getZ(),
				0,
				0,
				TYPE,
				stateId,
				Vec3.ZERO,
				0
		);
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
		return false;
	}

	@SuppressWarnings("unchecked")
	private static EntityType<BPFallingBlock> register(EntityType.Builder<?> builder) {
		if (!PhysExBootConfig.INSTANCE.isBlockPhysicsEnabled()) {
			return null;
		}
		ResourceKey<EntityType<?>> resourceKey = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(PhysEx.MOD_ID, "falling_block"));
		return (EntityType<BPFallingBlock>) Registry.register(BuiltInRegistries.ENTITY_TYPE, resourceKey, builder.build(resourceKey));
	}

	public static void init() {
	}

}
