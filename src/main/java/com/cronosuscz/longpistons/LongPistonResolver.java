package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import com.cronosuscz.longpistons.LongPistons;

import java.util.ArrayList;
import java.util.List;

public class LongPistonResolver {
    public static final int MAX_PUSH_LIMIT = 12;

    public static List<BlockPos> resolvePush(Level level, BlockPos pistonBasePos, Direction dir, int extensionLength) {
        List<BlockPos> blocksToMove = new ArrayList<>();

        BlockPos start = pistonBasePos.relative(dir, extensionLength); // start in front of piston head

        for (int i = 0; i < MAX_PUSH_LIMIT; i++) {
            BlockPos currentPos = start.relative(dir, i);
            BlockState state = level.getBlockState(currentPos);
            Block block = state.getBlock();

            // If air, skip (doesn't count toward 12 limit)
            if (state.isAir()) continue;

            // If unpushable (obsidian, bedrock, etc.)
            if (state.getPistonPushReaction() == PushReaction.BLOCK || !state.canSurvive(level, currentPos)) {
                return null;
            }

            blocksToMove.add(currentPos);

            if (blocksToMove.size() > MAX_PUSH_LIMIT) {
                return null; // Too many blocks
            }
        }

        return blocksToMove;
    }
}
