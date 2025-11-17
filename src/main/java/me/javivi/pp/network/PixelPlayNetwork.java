package me.javivi.pp.network;

import me.javivi.pp.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PixelPlayNetwork {
    private PixelPlayNetwork() {}

    public static void init() {
        // S2C payloads (server to client)
        PayloadTypeRegistry.playS2C().register(StartVideoPayload.ID, StartVideoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopVideoPayload.ID, StopVideoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartAudioPayload.ID, StartAudioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopAudioPayload.ID, StopAudioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartEasePayload.ID, StartEasePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartImagePayload.ID, StartImagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScreenVideoPayload.ID, ScreenVideoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScreenVolumePayload.ID, ScreenVolumePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScreenPausePayload.ID, ScreenPausePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenScreenControlPayload.ID, OpenScreenControlPayload.CODEC);
        
        // C2S payloads (client to server)
        PayloadTypeRegistry.playC2S().register(StartVideoC2SPayload.ID, StartVideoC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopVideoC2SPayload.ID, StopVideoC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartAudioC2SPayload.ID, StartAudioC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopAudioC2SPayload.ID, StopAudioC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartEaseC2SPayload.ID, StartEaseC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartImageC2SPayload.ID, StartImageC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScreenVolumeC2SPayload.ID, ScreenVolumeC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScreenPauseC2SPayload.ID, ScreenPauseC2SPayload.CODEC);
    }
}


