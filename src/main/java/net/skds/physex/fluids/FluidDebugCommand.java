package net.skds.physex.fluids;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.skds.physex.fluids.layer.FluidLayer;
import net.skds.physex.fluids.layer.SparseChunkStorage;

import java.util.Arrays;

public class FluidDebugCommand {
	public static void create(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("physex")
				.requires(c -> c.hasPermission(3))
				.then((Commands.literal("storage"))
						.executes(FluidDebugCommand::storage)));
	}

	private static int storage(CommandContext<CommandSourceStack> context) {
		try {

			Vec3 pos = context.getSource().getPosition();
			ServerLevel world = context.getSource().getLevel();

			BlockPos bp = BlockPos.containing(pos);

			StringBuilder sb = new StringBuilder();
			SparseChunkStorage<FluidState> storage = FluidLayer.get(world.getChunkAt(bp)).data();
			sb.append(Arrays.toString(storage.getStates()))
					.append("\n")
					.append(Arrays.toString(storage.getStateCount()));

			String msg = sb.toString();
			context.getSource().sendSuccess(() -> Component.literal(msg), false);
			return 1;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
