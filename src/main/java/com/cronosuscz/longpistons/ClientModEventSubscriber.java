package com.cronosuscz.longpistons.logic;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.cronosuscz.longpistons.LongPistons;

@Mod.EventBusSubscriber(modid = LongPistons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEventSubscriber {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(
                LongPistons.LONG_PISTON_MOVING_BLOCK_ENTITY.get(),
                LongPistonMovingBlockRenderer::new
            );
        });
    }
}
