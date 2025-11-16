package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StartImageC2SPayload(UUID targetPlayer, String url, boolean freeze, boolean white, double intro, double outro, double duration) implements CustomPayload {
    public static final Id<StartImageC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "start_image_c2s"));

    public static final PacketCodec<PacketByteBuf, StartImageC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
        buf.writeString(value.url);
        buf.writeBoolean(value.freeze);
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
        buf.writeDouble(value.duration);
    }, buf -> new StartImageC2SPayload(buf.readUuid(), buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

