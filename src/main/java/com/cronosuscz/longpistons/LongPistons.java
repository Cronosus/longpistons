package com.cronosuscz.longpistons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import com.cronosuscz.longpistons.logic.LongPistonBlock;
import com.cronosuscz.longpistons.logic.LongPistonMovingBlockRenderer;
import com.cronosuscz.longpistons.logic.LongPistonMovingBlockEntity;
import com.cronosuscz.longpistons.logic.ClientModEventSubscriber;
import com.cronosuscz.longpistons.logic.LongMovingPistonBlock;

@Mod(LongPistons.MODID)
public class LongPistons {
    public static final String MODID = "longpistons";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Block> PISTON_ARM = BLOCKS.register("piston_arm", PistonArmBlock::new);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final RegistryObject<Block> LONG_MOVING_PISTON = BLOCKS.register("long_moving_piston", LongMovingPistonBlock::new);
    public static final RegistryObject<BlockEntityType<LongPistonMovingBlockEntity>> LONG_PISTON_MOVING_BLOCK_ENTITY =
    BLOCK_ENTITIES.register("long_piston_moving_block_entity", () -> BlockEntityType.Builder.of(LongPistonMovingBlockEntity::new, LONG_MOVING_PISTON.get()).build(null));    
    
    public LongPistons() {
        MinecraftForge.EVENT_BUS.register(this);
        registerBlocksAndItems();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    public static void registerBlocksAndItems() {
        for (int length = 2; length <= 6; length++) {
            final int finalLength = length;
            String baseName = "long_piston_" + finalLength;
            String stickyName = "long_sticky_piston_" + finalLength;

            RegistryObject<Block> piston = BLOCKS.register(baseName, () -> new LongPistonBlock(false, finalLength));
            RegistryObject<Block> stickyPiston = BLOCKS.register(stickyName, () -> new LongPistonBlock(true, finalLength));
                      
            ITEMS.register(baseName, () -> new BlockItem(piston.get(), new Item.Properties()));
            ITEMS.register(stickyName, () -> new BlockItem(stickyPiston.get(), new Item.Properties()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientRenderer {

        @SubscribeEvent
        public static void onRenderWorld(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
            TextureAtlasSprite rodSprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("block/piston_top"));
            VertexConsumer consumer = buffer.getBuffer(RenderType.solid());

            for (BlockPos pos : BlockPos.betweenClosed(mc.player.blockPosition().offset(-32, -32, -32), mc.player.blockPosition().offset(32, 32, 32))) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof LongPistonBlock piston)) continue;
                if (!state.getValue(BlockStateProperties.EXTENDED)) continue;

                int len = piston.getExtensionLength();
                Direction dir = state.getValue(BlockStateProperties.FACING);

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                float min = 0.375f, max = 0.625f;

                for (int i = 1; i < len; i++) {
                    poseStack.pushPose();
                    poseStack.translate(dir.getStepX() * i, dir.getStepY() * i, dir.getStepZ() * i);
                    Matrix4f matrix = poseStack.last().pose();

                    for (Direction face : Direction.values()) {
                        float[] uvs = { rodSprite.getU0(), rodSprite.getV0(), rodSprite.getU1(), rodSprite.getV1() };
                        float x1 = face.getAxis() == Direction.Axis.X ? (face.getStepX() > 0 ? max : min) : min;
                        float y1 = face.getAxis() == Direction.Axis.Y ? (face.getStepY() > 0 ? max : min) : min;
                        float z1 = face.getAxis() == Direction.Axis.Z ? (face.getStepZ() > 0 ? max : min) : min;
                        float x2 = face.getAxis() == Direction.Axis.X ? (face.getStepX() > 0 ? max : min) : max;
                        float y2 = face.getAxis() == Direction.Axis.Y ? (face.getStepY() > 0 ? max : min) : max;
                        float z2 = face.getAxis() == Direction.Axis.Z ? (face.getStepZ() > 0 ? max : min) : max;

                        consumer.vertex(matrix, x1, y1, z1).color(255,255,255,255).uv(uvs[0], uvs[1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
                        consumer.vertex(matrix, x2, y1, z1).color(255,255,255,255).uv(uvs[2], uvs[1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
                        consumer.vertex(matrix, x2, y2, z2).color(255,255,255,255).uv(uvs[2], uvs[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
                        consumer.vertex(matrix, x1, y2, z2).color(255,255,255,255).uv(uvs[0], uvs[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
                    }
                    poseStack.popPose();
                }

                poseStack.popPose();
            }
        }
    }
}
