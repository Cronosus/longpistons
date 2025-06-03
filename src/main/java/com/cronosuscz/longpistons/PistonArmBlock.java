package com.cronosuscz.longpistons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import com.cronosuscz.longpistons.logic.LongPistonBlock;
import static com.cronosuscz.longpistons.logic.LongPistonBlock.EXTENDED;

public class PistonArmBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    public PistonArmBlock() {
        super(Properties.of().strength(-1.0F).noOcclusion().isRedstoneConductor((a, b, c) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK; // Like vanilla piston parts
    }

    public boolean isCollidable(BlockState state) {
        return true; // Important for collision
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape baseShape = Block.box(6, 6, -4, 10, 10, 12);
        return rotateShape(facing, baseShape);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape baseShape = Block.box(6, 6, -4, 10, 10, 12); // default shape facing south
        return rotateShape(facing, baseShape);
    }

    private static VoxelShape rotateShape(Direction direction, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        for (AABB box : shape.toAabbs()) {
            AABB rotated = switch (direction) {
                case NORTH -> new AABB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY, 1 - box.minZ);
                case SOUTH -> box;
                case WEST  -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
                case EAST  -> new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
                case UP    -> new AABB(box.minX, 1 - box.maxZ, box.minY, box.maxX, 1 - box.minZ, box.maxY);
                case DOWN  -> new AABB(box.minX, box.minZ, 1 - box.maxY, box.maxX, box.maxZ, 1 - box.minY);
            };
            buffer[1] = Shapes.or(buffer[1], Shapes.create(rotated));
        }

        return buffer[1];
    }
/*      //nefunguje spravne
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!isMoving && state.getBlock() != newState.getBlock()) {
            Direction dir = state.getValue(FACING);
            BlockPos current = pos;
            int steps = 0;

            // Walk backwards to find the piston base
            while (steps < 6) { // max expected extension
                current = current.relative(dir.getOpposite());
                BlockState checkState = level.getBlockState(current);
                if (checkState.getBlock() instanceof LongPistonBlock pistonBase) {
                    if (checkState.getValue(EXTENDED)) {
                        level.destroyBlock(current, true); // break piston base
                    }
                    break;
                } else if (!checkState.is(this)) {
                    break; // no more arm blocks in line
                }
                steps++;
            }
        }
    }*/
}
