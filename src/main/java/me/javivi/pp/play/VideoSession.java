package me.javivi.pp.play;

import me.javivi.pp.util.Easing;
import me.javivi.pp.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.player.PlayerAPI;
import me.javivi.pp.wm.CustomVideoPlayer;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;


public final class VideoSession {

    public enum EaseColor { BLACK, WHITE }

    private final MinecraftClient mc; 
    private final boolean freezeScreen;
    private final EaseColor easeColor;
    private final long introEaseMs;
    private final long outroEaseMs;
    private final Easing.Curve easeCurve;
    private final long startMs;

    private final CustomVideoPlayer player;
    private volatile boolean stopped;
    private volatile @Nullable String error;
    private volatile long firstFrameMs;
    private static final long PRE_ROLL_MS = 150L;
    private volatile long outroStartMs = -1L;

    public VideoSession(MinecraftClient mc,
                        String url,
                        boolean freezeScreen,
                        EaseColor easeColor,
                        double introEaseSeconds,
                        double outroEaseSeconds,
                        Easing.Curve easeCurve) {
        this.mc = mc;
        this.freezeScreen = freezeScreen;
        this.easeColor = easeColor;
        this.introEaseMs = TimeUtil.secondsToMillis(introEaseSeconds);
        this.outroEaseMs = TimeUtil.secondsToMillis(outroEaseSeconds);
        this.easeCurve = Objects.requireNonNullElse(easeCurve, Easing.Curve.EASE_IN_OUT_SINE);
        this.startMs = System.currentTimeMillis();
        this.firstFrameMs = 0L;


        String[] vlcArgs = new String[] {
                "--no-quiet",
                "--cr-average=5000",
                "--swscale-mode=0",
                "--network-caching=1500",
                "--live-caching=1500",
                "--file-caching=1500",
                "--aout", "directsound,waveout,mmdevice",
                "--vout", "none",
                "--avcodec-skip-idct=4",
                "--avcodec-fast",
                "--avcodec-hw", "none",
                "--no-metadata-network-access",
                "--no-file-logging",
                "--http-reconnect"
        };
        MediaPlayerFactory factory = new MediaPlayerFactory(vlcArgs);
        this.player = new CustomVideoPlayer(factory, r -> this.mc.execute(r));
        try {
            if (!PlayerAPI.isReady()) {
                this.error = "message.pixelplay.vlc_not_ready";
            } else {
                if (!freezeScreen) this.player.start(new URI(url));
                else this.player.startPaused(new URI(url));
            }
        } catch (URISyntaxException e) {
            this.error = "message.pixelplay.invalid_url";
        } catch (Throwable t) {
            this.error = t.getMessage();
        }
    }

    public boolean hasError() { return error != null; }
    public @Nullable String error() { return error; }

    public boolean isStopped() { return stopped; }

    public void stop() {
        if (stopped) return;
        stopped = true;
        try {
            if (player != null) player.stop();
        } catch (Throwable ignored) {}
        try {
            if (player != null) player.release();
        } catch (Throwable ignored) {}
    }


    public float introAlpha() {
        if (introEaseMs <= 0) return 1f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt >= introEaseMs) return 1f;
        float t = (float) dt / (float) introEaseMs;
        return Easing.ease(t, easeCurve);
    }

    public void markFirstFrame() {
        if (firstFrameMs == 0L) firstFrameMs = System.currentTimeMillis();
    }

    public float introAlphaFromFirstFrame() {
        if (introEaseMs <= 0) return 1f;
        if (firstFrameMs == 0L) return 0f;
        long dt = System.currentTimeMillis() - firstFrameMs;
        if (dt >= introEaseMs) return 1f;
        float t = (float) dt / (float) introEaseMs;
        return Easing.ease(t, easeCurve);
    }


    public float maskIntroAlphaNow() {
        if (introEaseMs <= 0) return 0f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt >= introEaseMs) return 0f;
        float t = (float) dt / (float) introEaseMs;
        return 1f - Easing.ease(t, easeCurve);
    }

    public float maskOutroAlphaNow() {
        return 0f;
    }

    public float preRollAlpha() {
        if (firstFrameMs != 0L) return 0f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt <= 0) return 0f;
        if (dt >= PRE_ROLL_MS) return 1f;
        return (float) dt / (float) PRE_ROLL_MS;
    }

    public boolean hasOutro() { return outroEaseMs > 0; }

    public void maybeStartOutro(long durationMs, long currentTimeMs, boolean playerEnded) {
        if (outroEaseMs <= 0) return;
        if (outroStartMs >= 0L) return;
        if (playerEnded) {
            outroStartMs = System.currentTimeMillis();
            return;
        }
        if (durationMs > 0) {
            long timeLeft = Math.max(0L, durationMs - Math.max(0L, currentTimeMs));
            if (timeLeft <= outroEaseMs) {
                outroStartMs = System.currentTimeMillis();
            }
        }
    }

    public float outroProgressMonotonic() {
        if (outroEaseMs <= 0 || outroStartMs < 0L) return 0f;
        long dt = System.currentTimeMillis() - outroStartMs;
        if (dt <= 0L) return 0f;
        if (dt >= outroEaseMs) return 1f;
        float t = (float) dt / (float) outroEaseMs;
        return Easing.ease(t, easeCurve);
    }

    public boolean isOutroFinished() {
        return outroEaseMs > 0 && outroStartMs >= 0L && (System.currentTimeMillis() - outroStartMs) >= outroEaseMs;
    }

    public boolean freezeScreen() { return freezeScreen; }
    public EaseColor easeColor() { return easeColor; }

    public CustomVideoPlayer player() { return player; }
}


