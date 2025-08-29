package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record StartEaseC2SPayload(UUID targetPlayer, boolean white, double intro, double total, double outro) implements CustomPayload {
    public static final Id<StartEaseC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "start_ease_c2s"));

    public static final PacketCodec<PacketByteBuf, StartEaseC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeUuid(value.targetPlayer);
        buf.writeBoolean(value.white);
        buf.writeDouble(value.intro);
        buf.writeDouble(value.total);
        buf.writeDouble(value.outro);
    }, buf -> new StartEaseC2SPayload(buf.readUuid(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
