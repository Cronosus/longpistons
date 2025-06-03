package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraftforge.common.extensions.IForgeBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.cronosuscz.longpistons.LongPistons;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class LongPistonBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;

    public final boolean isSticky;
    private final int extensionLength;

    public LongPistonBlock(boolean sticky, int length) {
        super(Properties.of().strength(1.5F));
        this.isSticky = sticky;
        this.extensionLength = length;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(EXTENDED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(EXTENDED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED);
    }

    public int getExtensionLength() {
        return extensionLength;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean extended = state.getValue(EXTENDED);

            if (powered && !extended) {
                level.scheduleTick(pos, this, 2);
            } else if (!powered && extended) {
                level.scheduleTick(pos, this, 2);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction dir = state.getValue(FACING);
        boolean powered = level.hasNeighborSignal(pos);

        if (powered && !state.getValue(EXTENDED)) {
            if (tryExtend(level, pos, dir)) {
                level.setBlock(pos, state.setValue(EXTENDED, true), 2);
            }
        } else if (!powered && state.getValue(EXTENDED)) {
            if (tryRetract(level, pos, dir)) {
                level.setBlock(pos, state.setValue(EXTENDED, false), 2);
            }
        }
    }
    
    private List<BlockPos> collectMovableBlocks(Level level, BlockPos pistonPos, Direction dir) {
        List<BlockPos> blocksToMove = new ArrayList<>();
        for (int i = 1; i <= extensionLength; i++) {
            BlockPos checkPos = pistonPos.relative(dir, i);
            BlockState targetState = level.getBlockState(checkPos);

            if (targetState.isAir()) continue;

            PushReaction reaction = targetState.getPistonPushReaction();
            if (reaction != PushReaction.NORMAL && reaction != PushReaction.PUSH_ONLY) {
                return Collections.emptyList(); // Abort if we hit an immovable block
            }

            blocksToMove.add(checkPos);

            if (blocksToMove.size() > 12) return Collections.emptyList(); // Vanilla limit
        }
        return blocksToMove;
    }
/*
    private void moveBlocks(Level level, List<BlockPos> blocksToMove, Direction dir, BlockPos pistonBase, boolean extending) {
        if (extending) {
            // Move from farthest to closest
            for (int i = blocksToMove.size() - 1; i >= 0; i--) {
                BlockPos from = blocksToMove.get(i);
                BlockPos to = from.relative(dir);
                BlockState stateToMove = level.getBlockState(from);
                PushReaction reaction = stateToMove.getPistonPushReaction();

                if (reaction == PushReaction.DESTROY) {
                    level.destroyBlock(from, true); // Drops items
                    continue; // Skip moving it
                }

                if (!stateToMove.isAir()) {
                    BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState()
                        .setValue(FACING, dir);

                    level.setBlock(to, movingState, 3);
                    int distance = pistonBase.distManhattan(from);
                    BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, stateToMove, dir, true, false, distance, isSticky);
                    level.setBlockEntity(be);
                    level.removeBlock(from, false);
                }
            }
        } else {
            // Handle retraction
            for (BlockPos from : blocksToMove) {
                BlockPos to = from.relative(dir.getOpposite());

                BlockState stateToMove = level.getBlockState(from);
                if (!stateToMove.isAir()) {
                    BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState()
                        .setValue(FACING, dir.getOpposite());

                    level.setBlock(to, movingState, 3);
                    int distance = pistonBase.distManhattan(from);
                    BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, stateToMove, dir.getOpposite(), false, false, distance, isSticky);
                    level.setBlockEntity(be);
                    level.removeBlock(from, false);
                }
            }
        }
    }
*/    
private void moveBlocks(Level level, List<BlockPos> blocksToMove, Direction dir, BlockPos pistonBase, boolean extending) {
    Collections.reverse(blocksToMove); // Move farthest blocks first 
    if (extending) {
        // From farthest to nearest
        for (int i = blocksToMove.size() - 1; i >= 0; i--) {
            BlockPos from = blocksToMove.get(i);
            BlockPos to = from.relative(dir);

            BlockState moved = level.getBlockState(from);
            if (!moved.isAir() && !moved.is(LongPistons.LONG_MOVING_PISTON.get())) {
                BlockState movingState = LongPistons.LONG_MOVING_PISTON.get()
                    .defaultBlockState()
                    .setValue(LongMovingPistonBlock.FACING, dir);
                level.setBlock(to, movingState, 3);

                BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(
                    to, moved, dir, true, false, 0, this.isSticky
                );
                level.setBlockEntity(be);
                level.removeBlock(from, false);
            }
        }
    } else {
        // Retraction logic (optional)
        for (BlockPos from : blocksToMove) {
            BlockPos to = from.relative(dir.getOpposite());

            BlockState moved = level.getBlockState(from);
            if (!moved.isAir() && !moved.is(LongPistons.LONG_MOVING_PISTON.get())) {
                BlockState movingState = LongPistons.LONG_MOVING_PISTON.get()
                    .defaultBlockState()
                    .setValue(LongMovingPistonBlock.FACING, dir.getOpposite());
                level.setBlock(to, movingState, 3);

                BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(
                    to, moved, dir.getOpposite(), false, false, 0, this.isSticky
                );
                level.setBlockEntity(be);
                level.removeBlock(from, false);
            }
        }
    }
}

/*
    private void moveBlocks(Level level, List<BlockPos> blocksToMove, Direction dir, BlockPos pistonBase, boolean extending) {
        Collections.reverse(blocksToMove); // Move farthest blocks first

        for (BlockPos from : blocksToMove) {
            BlockState moveState = level.getBlockState(from);
            PushReaction reaction = moveState.getPistonPushReaction();
            BlockPos to = from.relative(dir, extensionLength);

            if (reaction == PushReaction.DESTROY) {
                level.destroyBlock(from, true); // Drops items
                continue; // Skip moving it
            }

            BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState()
                .setValue(FACING, dir);

            level.setBlock(to, movingState, 3);

            int distance = pistonBase.distManhattan(from);
            BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, moveState, dir, extending, false, distance, isSticky);
            level.setBlockEntity(be);
            level.removeBlock(from, false);
        }
    }
*/
/*
    private boolean tryExtend(Level level, BlockPos pos, Direction dir) {
        //int length = this.getExtensionLength();
        List<BlockPos> blocksToMove = collectMovableBlocks(level, pos, dir);
        if (blocksToMove.isEmpty()) {
            return false;
        }
        //if (blocksToMove.isEmpty()) return false;

        // Check if blocks in front can be moved
        /*for (int i = 1; i <= length; i++) {
            BlockPos checkPos = pos.relative(dir, i);
            BlockState targetState = level.getBlockState(checkPos);
            PushReaction reaction = targetState.getPistonPushReaction();
            if (!targetState.isAir() && reaction != PushReaction.BLOCK && reaction != PushReaction.DESTROY) {
                return false;
            }
        }
*/

        // Move blocks with animation  
        /*for (int i = extensionLength; i >= 2; i--) {
            BlockPos from = pos.relative(dir, i - 1);
            BlockPos to = pos.relative(dir, i);

            BlockState moved = level.getBlockState(from);
            if (!moved.isAir()) {
                BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState()
                    .setValue(FACING, dir);

                level.setBlock(to, movingState, 3);
                BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, moved, dir, true, false, i, this.isSticky);
                level.setBlockEntity(be);
                level.removeBlock(from, false);
            }
        } */
/*        
         // Place piston arms
        for (int i = 1; i < extensionLength; i++) {
            BlockPos armPos = pos.relative(dir, i);
            level.setBlock(armPos, LongPistons.PISTON_ARM.get().defaultBlockState()
            .setValue(FACING, dir), 3);
        }

        // Piston head at the end
        BlockPos headPos = pos.relative(dir, extensionLength);
        BlockState headState = Blocks.PISTON_HEAD.defaultBlockState()
            .setValue(PistonHeadBlock.FACING, dir)
            .setValue(PistonHeadBlock.SHORT, false)
            .setValue(PistonHeadBlock.TYPE, Enum.valueOf(PistonHeadBlock.TYPE.getValueClass(), this.isSticky ? "STICKY" : "DEFAULT"));
        level.setBlock(headPos, headState, 3);

        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        return true;
    }
*/
private boolean tryExtend(Level level, BlockPos pos, Direction dir) {
    List<BlockPos> blocksToMove = LongPistonResolver.resolvePush(level, pos, dir, extensionLength);

    // Allow extension even when no blocks are pushed
    if (!blocksToMove.isEmpty()) {
        moveBlocks(level, blocksToMove, dir, pos, true);
    }

//if (blocksToMove == null) {
//    // Piston is blocked
//    return;
//}

// Move all blocks forward
for (int i = blocksToMove.size() - 1; i >= 0; i--) {
    BlockPos fromPos = blocksToMove.get(i);
    BlockState moveState = level.getBlockState(fromPos);
    BlockPos toPos = fromPos.relative(dir);

    // Replace with moving block
    BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(
    toPos, moveState, dir, true, false, 0, this.isSticky
    );
    level.setBlockEntity(be);

    level.removeBlock(fromPos, false);
}

    // Place piston arms in between base and head
    for (int i = 1; i < extensionLength; i++) {
        BlockPos armPos = pos.relative(dir, i);
        BlockState armState = LongPistons.PISTON_ARM.get().defaultBlockState()
            .setValue(FACING, dir);
        level.setBlock(armPos, armState, 3);
    }

    // Place piston head
    BlockPos headPos = pos.relative(dir, extensionLength);
    BlockState headState = Blocks.PISTON_HEAD.defaultBlockState()
        .setValue(PistonHeadBlock.FACING, dir)
        .setValue(PistonHeadBlock.SHORT, false)
        .setValue(PistonHeadBlock.TYPE, Enum.valueOf(PistonHeadBlock.TYPE.getValueClass(), this.isSticky ? "STICKY" : "DEFAULT"));
    level.setBlock(headPos, headState, 3);

    level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
    return true;
}

    private boolean tryRetract(Level level, BlockPos pos, Direction dir) {
        BlockPos headPos = pos.relative(dir, extensionLength);
        BlockState headState = level.getBlockState(headPos);
        if (headState.getBlock() instanceof PistonHeadBlock) {
            level.removeBlock(headPos, false);
        }

        // Animate pull back
/*        for (int i = 2; i < extensionLength; i++) {
            BlockPos from = pos.relative(dir, i);
            BlockPos to = pos.relative(dir, i - 1);

            BlockState pulled = level.getBlockState(from);
            if (!pulled.isAir()) {
                BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState()
                    .setValue(FACING, dir.getOpposite());

                level.setBlock(to, movingState, 3);
                BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, pulled, dir.getOpposite(), false, false, i, this.isSticky);
                level.setBlockEntity(be);
                level.removeBlock(from, false);
            }
        }*/

        // Remove arm blocks
        for (int i = 1; i < extensionLength; i++) {
            BlockPos armPos = pos.relative(dir, i);
            BlockState state = level.getBlockState(armPos);
            if (state.is(LongPistons.PISTON_ARM.get())) {
                level.removeBlock(armPos, false);
            }
        }
        
        if (isSticky) {
            BlockPos pullPos = pos.relative(dir, extensionLength);
            BlockState pulled = level.getBlockState(pullPos);
            PushReaction reaction = pulled.getPistonPushReaction();
            //if (!pulled.isAir() && reaction == PushReaction.NORMAL || reaction == PushReaction.PUSH_ONLY || reaction == PushReaction.DESTROY) {
            if (!pulled.isAir() && (reaction == PushReaction.NORMAL || reaction == PushReaction.PUSH_ONLY)) {
                /*BlockPos stickyTo = pos.relative(dir, extensionLength - 1);
                BlockPos to = pos.relative(dir, extensionLength - 1);
                BlockState movingState = LongPistons.LONG_MOVING_PISTON.get().defaultBlockState().setValue(FACING, dir.getOpposite());
                level.setBlock(to, movingState, 3);
                BlockEntity be = LongPistonMovingBlockEntity.newMovingBlockEntity(to, pulled, dir.getOpposite(), false, false, extensionLength, true);
                level.setBlockEntity(be);
                level.setBlock(stickyTo, pulled, 3);
                level.removeBlock(pullPos, false);*/
                moveBlocks(level, List.of(pullPos), dir.getOpposite(), pos, false);
            }
        }

        level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        return true;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    if (!level.isClientSide) return null;
        return (clientLevel, pos, blockState, blockEntity) ->
            LongPistonMovingBlockEntity.clientTick(clientLevel, pos, blockState, (LongPistonMovingBlockEntity) blockEntity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LongPistonMovingBlockEntity(pos, state);
    }
    
    protected static <E extends BlockEntity> BlockEntityTicker<E> createTickerHelper(
        BlockEntityType<E> actualType,
        BlockEntityType<? extends E> expectedType,
        BlockEntityTicker<? super E> ticker
    ) {
        return actualType == expectedType ? (BlockEntityTicker<E>) ticker : null;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!state.getValue(EXTENDED)) {
            return Shapes.block(); // Full cube
        }

        Direction facing = state.getValue(FACING);
        // Shrink base shape by 4px (4/16 = 0.25)
        return switch (facing) {
            case NORTH -> Block.box(0, 0, 4, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 12);
            case WEST -> Block.box(4, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 12, 16, 16);
            case UP -> Block.box(0, 0, 0, 16, 12, 16);
            case DOWN -> Block.box(0, 4, 0, 16, 16, 16);
        };
    }

    @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }
}
