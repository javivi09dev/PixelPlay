package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StopVideoPayload() implements CustomPayload {
    public static final Id<StopVideoPayload> ID = new Id<>(Identifier.of("pixelplay", "stop_video"));
    public static final PacketCodec<PacketByteBuf, StopVideoPayload> CODEC = PacketCodec.unit(new StopVideoPayload());
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}


