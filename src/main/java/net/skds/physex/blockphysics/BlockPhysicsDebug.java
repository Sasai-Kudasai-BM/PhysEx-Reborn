package net.skds.physex.blockphysics;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BlockPhysicsDebug {

	public static void debug(BlockPos pos, BlockState bs) {
		ClientLevel lvl = Minecraft.getInstance().level;
		Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, lvl) {
			{
				float scale = 1.05f;
				Vector3f scaleV = new Vector3f(scale);
				Vector3f offset = new Vector3f(scale - 1).mul(-.5f);
				setBlockState(bs);
				setBrightnessOverride(new Brightness(15, 15));
				setTransformation(new Transformation(offset, null, scaleV, null));
				setPos(Vec3.atLowerCornerOf(pos));
			}

			int age = 0;

			@Override
			public void tick() {
				super.tick();
				if (age++ > 2) {
					discard();
				}
			}
		};
		Minecraft.getInstance().schedule(() -> lvl.addEntity(display));
	}
}
