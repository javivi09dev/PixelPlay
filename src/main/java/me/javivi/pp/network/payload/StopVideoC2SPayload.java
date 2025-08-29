package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StopVideoC2SPayload(UUID targetPlayer) implements CustomPayload {
    public static final Id<StopVideoC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "stop_video_c2s"));

    public static final PacketCodec<PacketByteBuf, StopVideoC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
    }, buf -> new StopVideoC2SPayload(buf.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
