package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StopAudioC2SPayload(UUID targetPlayer) implements CustomPayload {
    public static final Id<StopAudioC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "stop_audio_c2s"));

    public static final PacketCodec<PacketByteBuf, StopAudioC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
    }, buf -> new StopAudioC2SPayload(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
