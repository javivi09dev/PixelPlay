package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartImagePayload(String url, boolean freeze, boolean white, double intro, double outro, double duration) implements CustomPayload {
    public static final Id<StartImagePayload> ID = new Id<>(Identifier.of("pixelplay", "start_image"));

    public static final PacketCodec<PacketByteBuf, StartImagePayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeString(value.url);
        buf.writeBoolean(value.freeze);
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.outro);
        buf.writeDouble(value.duration);
    }, buf -> new StartImagePayload(buf.readString(), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

