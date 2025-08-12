package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StopAudioPayload() implements CustomPayload {
    public static final Id<StopAudioPayload> ID = new Id<>(Identifier.of("pixelplay", "stop_audio"));
    public static final PacketCodec<PacketByteBuf, StopAudioPayload> CODEC = PacketCodec.unit(new StopAudioPayload());
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}


