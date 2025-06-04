package com.cronosuscz.longpistons.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import com.cronosuscz.longpistons.LongPistons;

public class LongPistonMovingBlockEntity extends BlockEntity {
    public BlockState movedState;
    public Direction direction;
    public boolean extending;
    public boolean isSticky;
    private int ticks;
    private final int maxTicks;

    // Default constructor for deserialization
    public LongPistonMovingBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, Blocks.AIR.defaultBlockState(), Direction.NORTH, true, 0, false);
    }

    // Full constructor for proper initialization
    public LongPistonMovingBlockEntity(BlockPos pos, BlockState pistonState, BlockState movedState, Direction direction, boolean extending, int stage, boolean sticky) {
        super(LongPistons.LONG_PISTON_MOVING_BLOCK_ENTITY.get(), pos, pistonState);
        this.movedState = movedState;
        this.direction = direction;
        this.extending = extending;
        this.ticks = stage;
        this.maxTicks = 10;
        this.isSticky = sticky;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LongPistonMovingBlockEntity be) {
        be.ticks++;
        if (be.ticks >= be.maxTicks) {
            if (be.extending) {
                level.setBlockAndUpdate(pos, be.movedState);
            } else {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            level.removeBlockEntity(pos);
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

    public static BlockEntity newMovingBlockEntity(BlockPos pos, BlockState pistonState, BlockState movedState, Direction dir, boolean extending, int stage, boolean sticky) {
        return new LongPistonMovingBlockEntity(pos, pistonState, movedState, dir, extending, stage, sticky);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ticks", ticks);
        tag.putBoolean("extending", extending);
        tag.putString("direction", direction.getName());
        tag.putBoolean("sticky", isSticky);
        if (movedState != null) {
            tag.put("movedState", NbtUtils.writeBlockState(movedState));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.ticks = tag.getInt("ticks");
        this.extending = tag.getBoolean("extending");
        this.isSticky = tag.getBoolean("sticky");

        if (tag.contains("direction")) {
            this.direction = Direction.byName(tag.getString("direction"));
            if (this.direction == null) {
                this.direction = Direction.NORTH; // fallback
            }
        } else {
            this.direction = Direction.NORTH;
        }

        if (tag.contains("movedState")) {
            this.movedState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("movedState"));
        } else {
            this.movedState = Blocks.AIR.defaultBlockState(); // fallback
        }
    }
}
