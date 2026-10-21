package net.skds.physex.blockphysics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

record BlockFallInfo(BlockPos pos, BlockPhysicsData physicsData, Direction direction, BlockState state) {

}
