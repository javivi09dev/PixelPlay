package me.javivi.pp.network;

import me.javivi.pp.network.payload.StartAudioPayload;
import me.javivi.pp.network.payload.StartEasePayload;
import me.javivi.pp.network.payload.StartVideoPayload;
import me.javivi.pp.network.payload.StopAudioPayload;
import me.javivi.pp.network.payload.StopVideoPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PixelPlayNetwork {
    private PixelPlayNetwork() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(StartVideoPayload.ID, StartVideoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopVideoPayload.ID, StopVideoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartAudioPayload.ID, StartAudioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopAudioPayload.ID, StopAudioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartEasePayload.ID, StartEasePayload.CODEC);
    }
}


