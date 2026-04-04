package me.javivi.pp.play;

import me.javivi.pp.util.Easing;
import me.javivi.pp.util.MediaUrlUtil;
import me.javivi.pp.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.players.MediaPlayer;

import java.util.Objects;

public final class ImageSession {
    public enum EaseColor { BLACK, WHITE }

    private final MinecraftClient mc;
    private final boolean freezeScreen;
    private final EaseColor easeColor;
    private final long introEaseMs;
    private final long outroEaseMs;
    private final long displayDurationMs;
    private final Easing.Curve easeCurve;
    private final long startMs;

    private volatile boolean stopped;
    private volatile @Nullable String error;
    private volatile long firstFrameMs;
    private static final long PRE_ROLL_MS = 150L;
    private volatile long outroStartMs = -1L;

    private final @Nullable MediaPlayer player;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public ImageSession(MinecraftClient mc,
                        String imageUrl,
                        boolean freezeScreen,
                        EaseColor easeColor,
                        double introEaseSeconds,
                        double outroEaseSeconds,
                        double displayDurationSeconds,
                        Easing.Curve easeCurve) {
        this.mc = mc;
        this.freezeScreen = freezeScreen;
        this.easeColor = easeColor;
        this.introEaseMs = TimeUtil.secondsToMillis(introEaseSeconds);
        this.outroEaseMs = TimeUtil.secondsToMillis(outroEaseSeconds);
        this.displayDurationMs = TimeUtil.secondsToMillis(displayDurationSeconds);
        this.easeCurve = Objects.requireNonNullElse(easeCurve, Easing.Curve.EASE_IN_OUT_SINE);
        this.startMs = System.currentTimeMillis();
        this.firstFrameMs = 0L;

        MediaPlayer built = null;
        try {
            if (MediaUrlUtil.isYoutubeWatchUrl(imageUrl)) {
                this.error = "message.pixelplay.youtube_unsupported";
            } else {
            MRL mrl = MediaAPI.getMRL(imageUrl);
            if (!mrl.await(30_000L)) {
                this.error = "message.pixelplay.mrl_timeout";
            } else if (mrl.error()) {
                this.error = "message.pixelplay.mrl_error";
            } else {
                built = mrl.createPlayer(Thread.currentThread(), r -> mc.execute(r), null, null, true, false);
                if (built == null) {
                    this.error = "message.pixelplay.player_create_failed";
                } else {
                    try {
                        if (!freezeScreen) built.start();
                        else built.startPaused();
                    } catch (Throwable t) {
                        try { built.release(); } catch (Throwable ignored) {}
                        built = null;
                        this.error = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                    }
                }
            }
            }
        } catch (Throwable t) {
            if (built != null) {
                try { built.release(); } catch (Throwable ignored) {}
                built = null;
            }
            if (this.error == null) {
                this.error = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            }
        }
        this.player = built;
    }

    /** Misma lógica que vídeo: tras el primer frame listo, sale de pausa si había freeze. */
    public void tickPlayback() {
        if (stopped || player == null || !freezeScreen) return;
        if (!player.playing() && player.paused() && player.width() > 1 && player.height() > 1) {
            player.resume();
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
        if (firstFrameMs == 0L && isLoaded()) firstFrameMs = System.currentTimeMillis();
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
        if (firstFrameMs != 0L || isLoaded()) return 0f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt <= 0) return 0f;
        if (dt >= PRE_ROLL_MS) return 1f;
        return (float) dt / (float) PRE_ROLL_MS;
    }

    public boolean hasOutro() { return outroEaseMs > 0 || displayDurationMs > 0; }

    public void maybeStartOutro() {
        if (outroStartMs >= 0L) return;

        long elapsed = System.currentTimeMillis() - startMs;

        if (displayDurationMs > 0 && elapsed >= displayDurationMs) {
            outroStartMs = System.currentTimeMillis();
            return;
        }

        if (displayDurationMs > 0 && outroEaseMs > 0) {
            long timeLeft = displayDurationMs - elapsed;
            if (timeLeft <= outroEaseMs && timeLeft > 0) {
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

    public boolean shouldStop() {
        if (displayDurationMs > 0) {
            long elapsed = System.currentTimeMillis() - startMs;
            if (elapsed >= displayDurationMs + outroEaseMs) {
                return true;
            }
        }
        return isOutroFinished();
    }

    public boolean freezeScreen() { return freezeScreen; }
    public EaseColor easeColor() { return easeColor; }

    /** Compatibilidad: WaterMedia 3 usa textura OpenGL directa vía {@link #getTextureIdFromRenderer()}. */
    public @Nullable Identifier textureId() { return null; }

    public boolean isLoaded() {
        return player != null && !player.error() && player.width() > 0 && player.height() > 0;
    }

    public int getImageWidth() { return imageWidth; }
    public int getImageHeight() { return imageHeight; }

    public void updateDimensions() {
        if (player != null && isLoaded()) {
            imageWidth = Math.max(1, player.width());
            imageHeight = Math.max(1, player.height());
        }
    }

    public int getTextureIdFromRenderer() {
        if (player == null) return 0;
        int t = player.texture();
        return t > 0 ? t : 0;
    }
}
