package me.javivi.pp.client;

import me.javivi.pp.client.gui.GuiVideoOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;


public final class ClientOverlayEvents implements HudRenderCallback {
    private final GuiVideoOverlay overlay;

    public ClientOverlayEvents(MinecraftClient mc, GuiVideoOverlay overlay) {
        this.overlay = overlay;
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        overlay.render(drawContext, tickCounter.getTickDelta(true));
    }
}


