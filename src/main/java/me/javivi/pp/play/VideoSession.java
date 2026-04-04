package me.javivi.pp.play;

import me.javivi.pp.util.Easing;
import me.javivi.pp.util.MediaUrlUtil;
import me.javivi.pp.util.TimeUtil;
import me.javivi.pp.wm.CustomVideoPlayer;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.players.FFMediaPlayer;
import org.watermedia.api.media.players.MediaPlayer;

import java.util.Objects;


public final class VideoSession {

    public enum EaseColor { BLACK, WHITE }

    private final boolean freezeScreen;
    private final EaseColor easeColor;
    private final long introEaseMs;
    private final long outroEaseMs;
    private final Easing.Curve easeCurve;
    private final long startMs;

    private final @Nullable CustomVideoPlayer player;
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
        this.freezeScreen = freezeScreen;
        this.easeColor = easeColor;
        this.introEaseMs = TimeUtil.secondsToMillis(introEaseSeconds);
        this.outroEaseMs = TimeUtil.secondsToMillis(outroEaseSeconds);
        this.easeCurve = Objects.requireNonNullElse(easeCurve, Easing.Curve.EASE_IN_OUT_SINE);
        this.startMs = System.currentTimeMillis();
        this.firstFrameMs = 0L;

        CustomVideoPlayer built = null;
        try {
            if (MediaUrlUtil.isYoutubeWatchUrl(url)) {
                this.error = "message.pixelplay.youtube_unsupported";
            } else if (!FFMediaPlayer.loaded()) {
                this.error = "message.pixelplay.vlc_not_ready";
            } else {
                MRL mrl = MediaAPI.getMRL(url);
                if (!mrl.await(30_000L)) {
                    this.error = "message.pixelplay.mrl_timeout";
                } else if (mrl.error()) {
                    this.error = "message.pixelplay.mrl_error";
                } else {
                    Thread renderThread = Thread.currentThread();
                    MediaPlayer mp = mrl.createPlayer(renderThread, r -> mc.execute(r), null, null, true, true);
                    if (mp == null) {
                        this.error = "message.pixelplay.player_create_failed";
                    } else {
                        built = new CustomVideoPlayer(mp);
                        try {
                            if (!freezeScreen) mp.start();
                            else mp.startPaused();
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

    public @Nullable CustomVideoPlayer player() { return player; }
}

