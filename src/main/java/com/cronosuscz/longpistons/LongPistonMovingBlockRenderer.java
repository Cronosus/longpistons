package com.cronosuscz.longpistons.logic;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.properties.PistonType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.cronosuscz.longpistons.LongPistons;

@OnlyIn(Dist.CLIENT)
public class LongPistonMovingBlockRenderer implements BlockEntityRenderer<LongPistonMovingBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public LongPistonMovingBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LongPistonMovingBlockEntity be, float partialTicks, PoseStack poseStack,
        MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        BlockState state = be.movedState;
        if (state == null || be.direction == null) return;

        float progress = be.getProgress(partialTicks);
        Direction dir = be.direction;
        float offset = (be.isExtending() ? progress : 1.0f - progress);

        // Translate to where the head (moved block) should be
        poseStack.pushPose();
        poseStack.translate(
            dir.getStepX() * offset,
            dir.getStepY() * offset,
            dir.getStepZ() * offset
        );
        dispatcher.renderSingleBlock(state, poseStack, buffer, combinedLight, combinedOverlay);
        poseStack.popPose();

        // Render the piston arm (between base and head)
        if (be.extending && progress < 1.0f) {
            poseStack.pushPose();

            // Position the arm slightly behind the head
            float armOffset = progress - 1.0f;
            poseStack.translate(
                dir.getStepX() * armOffset,
                dir.getStepY() * armOffset,
                dir.getStepZ() * armOffset
            );

            // Use vanilla piston head block to simulate the arm visuals
            BlockState armState = LongPistons.PISTON_ARM.get().defaultBlockState();
            poseStack.pushPose();

            double adjustedarmOffset = offset - 1;  // or adjust based on how piston length is handled
            poseStack.translate(dir.getStepX() * armOffset, dir.getStepY() * armOffset, dir.getStepZ() * armOffset);

            dispatcher.renderSingleBlock(armState, poseStack, buffer, combinedLight, combinedOverlay);
            poseStack.popPose();
        }
    }
}
