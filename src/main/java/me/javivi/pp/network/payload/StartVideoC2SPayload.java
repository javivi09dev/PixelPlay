package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StartVideoC2SPayload(UUID targetPlayer, String url, boolean freeze, boolean white, double intro, double outro) implements CustomPayload {
    public static final Id<StartVideoC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "start_video_c2s"));

    public static final PacketCodec<PacketByteBuf, StartVideoC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
        buf.writeString(value.url);
        buf.writeBoolean(value.freeze);
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
    }, buf -> new StartVideoC2SPayload(buf.readUuid(), buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
