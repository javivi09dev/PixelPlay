package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ScreenVideoPayload(BlockPos pos, String url, boolean loop, BlockPos min, BlockPos max) implements CustomPayload {
    public static final Id<ScreenVideoPayload> ID = new Id<>(Identifier.of("pixelplay", "screen_video"));

    public static final PacketCodec<PacketByteBuf, ScreenVideoPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.pos);
        buf.writeString(value.url != null ? value.url : "");
        buf.writeBoolean(value.loop);
        buf.writeBoolean(value.min != null && value.max != null);
        if (value.min != null && value.max != null) {
            buf.writeBlockPos(value.min);
            buf.writeBlockPos(value.max);
        }
    }, buf -> {
        BlockPos pos = buf.readBlockPos();
        String url = buf.readString();
        boolean loop = buf.readBoolean();
        boolean hasArea = buf.readBoolean();
        BlockPos min = null;
        BlockPos max = null;
        if (hasArea) {
            min = buf.readBlockPos();
            max = buf.readBlockPos();
        }
        return new ScreenVideoPayload(pos, url.isEmpty() ? null : url, loop, min, max);
    });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

