package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartVideoPayload(String url, boolean freeze, boolean white, double intro, double outro) implements CustomPayload {
    public static final Id<StartVideoPayload> ID = new Id<>(Identifier.of("pixelplay", "start_video"));

    public static final PacketCodec<PacketByteBuf, StartVideoPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeString(value.url);
        buf.writeBoolean(value.freeze);
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
    }, buf -> new StartVideoPayload(buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}


