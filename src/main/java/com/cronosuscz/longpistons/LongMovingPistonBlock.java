package com.cronosuscz.longpistons.logic;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
import com.cronosuscz.longpistons.LongPistons;

public class LongMovingPistonBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    public LongMovingPistonBlock() {
        super(Properties.copy(Blocks.PISTON).noOcclusion().strength(-1.0F).pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // invisible so the BE renderer draws it
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LongPistonMovingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != LongPistons.LONG_PISTON_MOVING_BLOCK_ENTITY.get()) {
            return null;
        }
        if (!level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof LongPistonMovingBlockEntity movingBlock) {
                    LongPistonMovingBlockEntity.serverTick(lvl, pos, st, movingBlock);
                }
            };
        } else {
            return (lvl, pos, st, be) -> {
                if (be instanceof LongPistonMovingBlockEntity movingBlock) {
                    LongPistonMovingBlockEntity.clientTick(lvl, pos, st, movingBlock);
                }
            };
        }
    }

    @Override
    public boolean isAir(BlockState state) {
        return true; // treated like air for physics & placement
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
