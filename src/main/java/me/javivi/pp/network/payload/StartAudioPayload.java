package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartAudioPayload(String url, double intro, double outro) implements CustomPayload {
    public static final Id<StartAudioPayload> ID = new Id<>(Identifier.of("pixelplay", "start_audio"));
    public static final PacketCodec<PacketByteBuf, StartAudioPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeString(value.url);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
    }, buf -> new StartAudioPayload(buf.readString(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}


