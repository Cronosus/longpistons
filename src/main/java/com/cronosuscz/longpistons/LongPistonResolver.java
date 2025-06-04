package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import com.cronosuscz.longpistons.LongPistons;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class LongPistonResolver {
    public static final int MAX_PUSH_LIMIT = 12;
    public static final Set<Block> BLOCK_PUSH_BLACKLIST = Set.of(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.REINFORCED_DEEPSLATE, Blocks.END_PORTAL_FRAME);

public static List<BlockPos> resolvePush(Level level, BlockPos pistonPos, Direction dir, int extensionLength) {
    List<BlockPos> blocksToMove = new ArrayList<>();
    List<BlockPos> airSlots = new ArrayList<>();

    // First, collect the blocks in front of the piston up to the piston head
    for (int i = 1; i <= extensionLength; i++) {
        BlockPos currentPos = pistonPos.relative(dir, i);
        BlockState state = level.getBlockState(currentPos);
        Block block = state.getBlock();

        if (state.isAir()) continue;

        if (BLOCK_PUSH_BLACKLIST.contains(block)
                || block == LongPistons.PISTON_ARM.get()
                || state.getPistonPushReaction() == PushReaction.BLOCK
                || !state.canSurvive(level, currentPos)) {
            return List.of(); // Unpushable in path
        }

        blocksToMove.add(currentPos);
    }

    // Then, collect the space into which those blocks will move
    for (int i = extensionLength + 1; i <= extensionLength + MAX_PUSH_LIMIT; i++) {
        BlockPos targetPos = pistonPos.relative(dir, i);
        if (level.getBlockState(targetPos).isAir()) {
            airSlots.add(targetPos);
        } else {
            break; // Stop if we hit a non-air block
        }
    }

    // If not enough space or too many blocks, cancel
    if (blocksToMove.size() > MAX_PUSH_LIMIT || blocksToMove.size() > airSlots.size()) {
        return null;
    }

    // Push is valid
    return blocksToMove;
}

}
