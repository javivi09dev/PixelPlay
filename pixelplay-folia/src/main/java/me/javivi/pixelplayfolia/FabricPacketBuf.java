package me.javivi.pixelplayfolia;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal writer matching Minecraft {@code PacketByteBuf} encoding (VarInt length UTF-8 strings).
 */
public final class FabricPacketBuf {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void writeBoolean(boolean v) {
        out.write(v ? 1 : 0);
    }

    public void writeDouble(double v) {
        long bits = Double.doubleToRawLongBits(v);
        for (int i = 7; i >= 0; i--) {
            out.write((int) ((bits >>> (i * 8)) & 0xFF));
        }
    }

    public void writeVarInt(int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.write(value);
                return;
            }
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public void writeString(String s) {
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        if (utf8.length > 32767) {
            throw new IllegalArgumentException("String too long for MC packet");
        }
        writeVarInt(utf8.length);
        out.write(utf8, 0, utf8.length);
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
