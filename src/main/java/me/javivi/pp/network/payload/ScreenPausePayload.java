package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ScreenPausePayload(BlockPos pos, boolean paused) implements CustomPayload {
    public static final Id<ScreenPausePayload> ID = new Id<>(Identifier.of("pixelplay", "screen_pause"));

    public static final PacketCodec<PacketByteBuf, ScreenPausePayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.pos);
        buf.writeBoolean(value.paused);
    }, buf -> {
        BlockPos pos = buf.readBlockPos();
        boolean paused = buf.readBoolean();
        return new ScreenPausePayload(pos, paused);
    });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

