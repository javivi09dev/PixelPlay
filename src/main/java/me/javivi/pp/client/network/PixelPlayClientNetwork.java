package me.javivi.pp.client.network;

import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.play.EaseSession;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.AudioSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.network.payload.StartVideoPayload;
import me.javivi.pp.network.payload.StartEasePayload;
import me.javivi.pp.network.payload.StopVideoPayload;
import me.javivi.pp.network.payload.StartAudioPayload;
import me.javivi.pp.network.payload.StopAudioPayload;
import me.javivi.pp.network.payload.StartImagePayload;
import me.javivi.pp.util.Easing;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;


public final class PixelPlayClientNetwork {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(StartVideoPayload.ID, (payload, context) -> {
            if (payload instanceof StartVideoPayload p) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    var color = p.white() ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK;
                    var session = new VideoSession(mc, p.url(), p.freeze(), color, p.intro(), p.outro(), Easing.Curve.EASE_IN_OUT_SINE);
                    PixelplayClient.setVideoSession(session);
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StartEasePayload.ID, (payload, context) -> {
            if (payload instanceof StartEasePayload p) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> PixelplayClient.setEase(new EaseSession(p.white(), p.intro(), p.total(), p.outro(), Easing.Curve.EASE_IN_OUT_SINE)));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StopVideoPayload.ID, (payload, context) -> {
            if (payload instanceof StopVideoPayload) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    PlaybackManager.stopVideo();
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StopAudioPayload.ID, (payload, context) -> {
            if (payload instanceof StopAudioPayload) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> PlaybackManager.stopAudio());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StartAudioPayload.ID, (payload, context) -> {
            if (payload instanceof StartAudioPayload p) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    AudioSession session = new AudioSession(p.url(), p.intro(), p.outro(), Easing.Curve.EASE_IN_OUT_SINE);
                    if (!session.hasError()) {
                        PlaybackManager.setAudioSession(session);
                    }
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StartImagePayload.ID, (payload, context) -> {
            if (payload instanceof StartImagePayload p) {
                var mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    var color = p.white() ? ImageSession.EaseColor.WHITE : ImageSession.EaseColor.BLACK;
                    var session = new ImageSession(mc, p.url(), p.freeze(), color, p.intro(), p.outro(), p.duration(), Easing.Curve.EASE_IN_OUT_SINE);
                    PixelplayClient.setImageSession(session);
                });
            }
        });
    }
}


