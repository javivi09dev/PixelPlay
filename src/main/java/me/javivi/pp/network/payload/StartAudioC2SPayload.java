package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StartAudioC2SPayload(UUID targetPlayer, String url, double intro, double outro) implements CustomPayload {
    public static final Id<StartAudioC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "start_audio_c2s"));

    public static final PacketCodec<PacketByteBuf, StartAudioC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
        buf.writeString(value.url);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
    }, buf -> new StartAudioC2SPayload(buf.readUuid(), buf.readString(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
