package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import com.cronosuscz.longpistons.LongPistons;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class LongPistonResolver {
    public static final int MAX_PUSH_LIMIT = 12;
    public static final Set<Block> BLOCK_PUSH_BLACKLIST = Set.of(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.REINFORCED_DEEPSLATE, Blocks.END_PORTAL_FRAME);

 public static List<BlockPos> resolvePush(Level level, BlockPos pistonPos, Direction dir, int extensionLength) {
    List<BlockPos> allInLine = new ArrayList<>();
    int airSlots = 0;

    int totalCheckRange = extensionLength + 12; // check full extension + max vanilla push

    for (int i = 1; i <= totalCheckRange; i++) {
        BlockPos currentPos = pistonPos.relative(dir, i);
        BlockState state = level.getBlockState(currentPos);
        Block block = state.getBlock();

        // Stop at unpushable block (don't include it)
        if (BLOCK_PUSH_BLACKLIST.contains(block)
                || state.getPistonPushReaction() == PushReaction.BLOCK
                || !state.canSurvive(level, currentPos)) {
            break;
        }

        allInLine.add(currentPos);

        if (state.isAir()) {
            airSlots++;
        }
    }

    // Determine how many blocks we can push (up to 12)
    int maxMovable = Math.min(airSlots, MAX_PUSH_LIMIT);

    // Filter only pushable, non-air blocks (respect order)
    List<BlockPos> blocksToMove = allInLine.stream()
        .filter(pos -> !level.getBlockState(pos).isAir())
        .limit(maxMovable)
        .collect(Collectors.toList());

    System.out.println("Air slots: " + airSlots);
    System.out.println("Pushable blocks: " + blocksToMove.stream().map(BlockPos::toShortString).toList());
    return blocksToMove;
}
}
