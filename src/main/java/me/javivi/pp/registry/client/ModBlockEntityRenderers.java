package me.javivi.pp.registry.client;

import me.javivi.pp.client.render.ScreenBlockEntityRenderer;
import me.javivi.pp.registry.ModBlockEntities;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class ModBlockEntityRenderers {
    private ModBlockEntityRenderers() {}

    public static void registerClient() {
        BlockEntityRendererFactories.register(ModBlockEntities.SCREEN, ScreenBlockEntityRenderer::new);
    }
}


