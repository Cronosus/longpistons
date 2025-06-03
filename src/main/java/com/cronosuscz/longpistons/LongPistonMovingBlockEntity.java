package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import com.cronosuscz.longpistons.LongPistons;

public class LongPistonMovingBlockEntity extends BlockEntity {
    public BlockState movedState;
    public Direction direction;
    public boolean extending;
    public boolean isSticky;
    private int ticks;
    private final int maxTicks;

    public LongPistonMovingBlockEntity(BlockPos pos, BlockState state) {
        super(LongPistons.LONG_PISTON_MOVING_BLOCK_ENTITY.get(), pos, state);
        this.ticks = 0;
        this.maxTicks = 10; // Duration of animation in ticks
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LongPistonMovingBlockEntity be) {
        be.ticks++;
        if (be.ticks >= be.maxTicks) {
            if (be.extending) {
                level.setBlockAndUpdate(pos, be.movedState);
                level.removeBlockEntity(pos);
            } else {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.removeBlockEntity(pos);
            }
        } else {
            level.sendBlockUpdated(pos, state, state, 0);
        }    
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LongPistonMovingBlockEntity be) {
        be.ticks++;
        if (be.ticks >= be.maxTicks) {
            level.setBlockAndUpdate(pos, be.extending ? be.movedState : Blocks.AIR.defaultBlockState());
            level.removeBlockEntity(pos);
        }
    }

    public float getProgress(float partialTicks) {
        return Math.min(1.0f, (ticks + partialTicks) / maxTicks);
    }
    
    public boolean isExtending() {
        return extending;
    }

    public static BlockEntity newMovingBlockEntity(BlockPos pos, BlockState state, Direction dir, boolean extending, boolean source, int stage, boolean sticky) {
        LongPistonMovingBlockEntity be = new LongPistonMovingBlockEntity(pos, state);
        be.movedState = state;
        be.direction = dir;
        be.extending = extending;
        be.ticks = stage;
        be.isSticky = sticky;
        return be;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("movedState", NbtUtils.writeBlockState(movedState));
        tag.putInt("ticks", ticks);
        tag.putBoolean("extending", extending);
        tag.putString("direction", direction.getName());
        tag.putBoolean("sticky", isSticky);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.movedState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("movedState"));
        this.ticks = tag.getInt("ticks");
        this.extending = tag.getBoolean("extending");
        this.direction = Direction.byName(tag.getString("direction"));
        this.isSticky = tag.getBoolean("sticky");
    }
}
