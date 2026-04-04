package me.javivi.pixelplayfolia;

/**
 * S2C payload bodies matching {@code me.javivi.pp.network.payload} Fabric codecs (channel = pixelplay:&lt;path&gt;).
 */
public final class PixelPlayPayloads {
    public static final String CH_START_VIDEO = "pixelplay:start_video";
    public static final String CH_STOP_VIDEO = "pixelplay:stop_video";
    public static final String CH_START_AUDIO = "pixelplay:start_audio";
    public static final String CH_STOP_AUDIO = "pixelplay:stop_audio";
    public static final String CH_START_EASE = "pixelplay:start_ease";
    public static final String CH_START_IMAGE = "pixelplay:start_image";

    private PixelPlayPayloads() {}

    public static byte[] startVideo(String url, boolean freeze, boolean white, double intro, double outro) {
        FabricPacketBuf b = new FabricPacketBuf();
        b.writeString(url);
        b.writeBoolean(freeze);
        b.writeBoolean(white);
        b.writeDouble(intro);
        b.writeDouble(outro);
        return b.toByteArray();
    }

    public static byte[] stopVideo() {
        return new byte[0];
    }

    public static byte[] startAudio(String url, double intro, double outro) {
        FabricPacketBuf b = new FabricPacketBuf();
        b.writeString(url);
        b.writeDouble(intro);
        b.writeDouble(outro);
        return b.toByteArray();
    }

    public static byte[] stopAudio() {
        return new byte[0];
    }

    public static byte[] startEase(boolean white, double intro, double total, double outro) {
        FabricPacketBuf b = new FabricPacketBuf();
        b.writeBoolean(white);
        b.writeDouble(intro);
        b.writeDouble(total);
        b.writeDouble(outro);
        return b.toByteArray();
    }

    public static byte[] startImage(String url, boolean freeze, boolean white, double intro, double outro, double duration) {
        FabricPacketBuf b = new FabricPacketBuf();
        b.writeString(url);
        b.writeBoolean(freeze);
        b.writeBoolean(white);
        b.writeDouble(intro);
        b.writeDouble(outro);
        b.writeDouble(duration);
        return b.toByteArray();
    }
}
