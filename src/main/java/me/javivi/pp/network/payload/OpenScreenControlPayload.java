package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenScreenControlPayload(BlockPos pos) implements CustomPayload {
    public static final Id<OpenScreenControlPayload> ID = new Id<>(Identifier.of("pixelplay", "open_screen_control"));

    public static final PacketCodec<PacketByteBuf, OpenScreenControlPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.pos);
    }, buf -> {
        BlockPos pos = buf.readBlockPos();
        return new OpenScreenControlPayload(pos);
    });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

