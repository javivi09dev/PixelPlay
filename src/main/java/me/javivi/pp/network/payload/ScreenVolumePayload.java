package me.javivi.pp.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ScreenVolumePayload(BlockPos pos, float volume) implements CustomPayload {
    public static final Id<ScreenVolumePayload> ID = new Id<>(Identifier.of("pixelplay", "screen_volume"));

    public static final PacketCodec<PacketByteBuf, ScreenVolumePayload> CODEC = PacketCodec.of((value, buf) -> {
        buf.writeBlockPos(value.pos);
        buf.writeFloat(value.volume);
    }, buf -> {
        BlockPos pos = buf.readBlockPos();
        float volume = buf.readFloat();
        return new ScreenVolumePayload(pos, Math.max(0f, Math.min(1f, volume))); // Clamp 0-1
    });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

