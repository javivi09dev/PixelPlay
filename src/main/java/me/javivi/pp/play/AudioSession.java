package me.javivi.pp.play;

import me.javivi.pp.sound.MultimediaVolume;
import me.javivi.pp.util.Easing;
import me.javivi.pp.util.MediaUrlUtil;
import me.javivi.pp.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.players.FFMediaPlayer;
import org.watermedia.api.media.players.MediaPlayer;


public final class AudioSession {
    private final @Nullable MediaPlayer player;
    private final long introMs;
    private final long outroMs;
    private final Easing.Curve easeCurve;
    private final long startMs;
    private volatile boolean stopping;
    private volatile long stopStartMs = -1L;
    private volatile boolean stopped;
    private volatile @Nullable String error;

    public AudioSession(String url, double introSeconds, double outroSeconds, Easing.Curve curve) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MediaPlayer built = null;
        this.introMs = TimeUtil.secondsToMillis(introSeconds);
        this.outroMs = TimeUtil.secondsToMillis(outroSeconds);
        this.easeCurve = curve != null ? curve : Easing.Curve.EASE_IN_OUT_SINE;
        this.startMs = System.currentTimeMillis();

        try {
            if (MediaUrlUtil.isYoutubeWatchUrl(url)) {
                this.error = "message.pixelplay.youtube_unsupported";
            } else if (mc == null) {
                this.error = "message.pixelplay.client_not_ready";
            } else if (!FFMediaPlayer.loaded()) {
                this.error = "message.pixelplay.vlc_not_ready";
            } else {
                MRL mrl = MediaAPI.getMRL(url);
                if (!mrl.await(30_000L)) {
                    this.error = "message.pixelplay.mrl_timeout";
                } else if (mrl.error()) {
                    this.error = "message.pixelplay.mrl_error";
                } else {
                    built = mrl.createPlayer(Thread.currentThread(), r -> mc.execute(r), null, null, false, true);
                    if (built == null) {
                        this.error = "message.pixelplay.player_create_failed";
                    } else {
                        try {
                            built.start();
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

    public void tick() {
        if (stopped) return;
        if (player == null) return;
        int volumeTarget = Math.round(100f * MultimediaVolume.getMasterMultiplier());
        long now = System.currentTimeMillis();
        if (!stopping) {
            if (introMs > 0) {
                long dt = now - startMs;
                if (dt < introMs) {
                    float t = (float) dt / (float) introMs;
                    float k = Easing.ease(t, easeCurve);
                    player.volume(Math.round(k * volumeTarget));
                    return;
                }
            }
            player.volume(volumeTarget);
        } else {
            if (stopStartMs < 0L) stopStartMs = now;
            if (outroMs > 0) {
                long dt = now - stopStartMs;
                long used = Math.max(0L, Math.min(outroMs, dt));
                float t = (float) used / (float) outroMs;
                float k = 1.0f - Easing.ease(t, easeCurve);
                int v = Math.round(k * volumeTarget);
                player.volume(v);
                if (used >= outroMs) {
                    stopNow();
                }
            } else {
                stopNow();
            }
        }
    }

    public void requestStop() { this.stopping = true; }

    public boolean isStopped() { return stopped; }

    private void stopNow() {
        if (stopped) return;
        stopped = true;
        if (player == null) return;
        try { player.stop(); } catch (Throwable ignored) {}
        try { player.release(); } catch (Throwable ignored) {}
    }
}
