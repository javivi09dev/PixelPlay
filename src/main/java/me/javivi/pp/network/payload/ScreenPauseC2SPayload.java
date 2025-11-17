package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ScreenPauseC2SPayload(BlockPos pos, boolean paused) implements CustomPayload {
    public static final Id<ScreenPauseC2SPayload> ID = new Id<>(Identifier.of("pixelplay", "screen_pause_c2s"));

    public static final PacketCodec<PacketByteBuf, ScreenPauseC2SPayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.pos);
        buf.writeBoolean(value.paused);
    }, buf -> {
        BlockPos pos = buf.readBlockPos();
        boolean paused = buf.readBoolean();
        return new ScreenPauseC2SPayload(pos, paused);
    });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

