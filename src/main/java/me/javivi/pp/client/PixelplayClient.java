package me.javivi.pp.client;

import me.javivi.pp.client.gui.GuiVideoOverlay;
import me.javivi.pp.client.network.PixelPlayClientNetwork;
import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.play.EaseSession;
import me.javivi.pp.client.gui.FreezeScreen;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.ImageSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class PixelplayClient implements ClientModInitializer {
    private static GuiVideoOverlay OVERLAY;

    @Override
    public void onInitializeClient() {
        MinecraftClient mc = MinecraftClient.getInstance();
        OVERLAY = new GuiVideoOverlay(mc);
        HudRenderCallback.EVENT.register(new ClientOverlayEvents(mc, OVERLAY));
        PixelPlayClientNetwork.init();
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickHandler());
        me.javivi.pp.registry.client.ModBlockEntityRenderers.registerClient();
    }

    public static void setVideoSession(VideoSession session) {
        PlaybackManager.setVideoSession(session);
        if (OVERLAY != null) OVERLAY.setSession(session);
        var mc = MinecraftClient.getInstance();
        if (session != null && session.freezeScreen()) {
            if (mc.currentScreen == null || !(mc.currentScreen instanceof FreezeScreen)) {
                mc.setScreen(new FreezeScreen());
            }
        } else {
            if (mc.currentScreen instanceof FreezeScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static void setEase(EaseSession ease) {
        if (OVERLAY != null) OVERLAY.setEase(ease);
    }

    public static void setImageSession(ImageSession session) {
        PlaybackManager.setImageSession(session);
        if (OVERLAY != null) OVERLAY.setImageSession(session);
        var mc = MinecraftClient.getInstance();
        if (session != null && session.freezeScreen()) {
            if (mc.currentScreen == null || !(mc.currentScreen instanceof FreezeScreen)) {
                mc.setScreen(new FreezeScreen());
            }
        } else {
            if (mc.currentScreen instanceof FreezeScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static GuiVideoOverlay getOverlay() {
        return OVERLAY;
    }
}
