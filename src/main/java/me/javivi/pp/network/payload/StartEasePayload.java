package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartEasePayload(boolean white, double intro, double total, double outro) implements CustomPayload {
    public static final Id<StartEasePayload> ID = new Id<>(Identifier.of("pixelplay", "start_ease"));
    public static final PacketCodec<PacketByteBuf, StartEasePayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.total);
        buf.writeDouble(value.outro);
    }, buf -> new StartEasePayload(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}


