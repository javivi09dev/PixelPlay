package me.javivi.pp.play;

import me.javivi.pp.sound.MultimediaVolume;
import me.javivi.pp.util.Easing;
import me.javivi.pp.util.TimeUtil;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.player.PlayerAPI;
import org.watermedia.api.player.videolan.MusicPlayer;

import java.net.URI;
import java.net.URISyntaxException;


public final class AudioSession {
    private final MusicPlayer player;
    private final long introMs;
    private final long outroMs;
    private final Easing.Curve easeCurve;
    private final long startMs;
    private volatile boolean stopping;
    private volatile long stopStartMs = -1L;
    private volatile boolean stopped;
    private volatile @Nullable String error;

    public AudioSession(String url, double introSeconds, double outroSeconds, Easing.Curve curve) {
        this.player = new MusicPlayer(PlayerAPI.getFactorySoundOnly());
        this.introMs = TimeUtil.secondsToMillis(introSeconds);
        this.outroMs = TimeUtil.secondsToMillis(outroSeconds);
        this.easeCurve = curve != null ? curve : Easing.Curve.EASE_IN_OUT_SINE;
        this.startMs = System.currentTimeMillis();
        try {
            if (!PlayerAPI.isReady()) {
                this.error = "message.pixelplay.vlc_not_ready";
            } else {
                this.player.start(new URI(url));
            }
        } catch (URISyntaxException e) {
            this.error = "message.pixelplay.invalid_url";
        } catch (Throwable t) {
            this.error = t.getMessage();
        }
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
                    player.setVolume(Math.round(k * volumeTarget));
                    return;
                }
            }
            player.setVolume(volumeTarget);
        } else {
            if (stopStartMs < 0L) stopStartMs = now;
            if (outroMs > 0) {
                long dt = now - stopStartMs;
                long used = Math.max(0L, Math.min(outroMs, dt));
                float t = (float) used / (float) outroMs;
                float k = 1.0f - Easing.ease(t, easeCurve);
                int v = Math.round(k * volumeTarget);
                player.setVolume(v);
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
        try { player.stop(); } catch (Throwable ignored) {}
        try { player.release(); } catch (Throwable ignored) {}
    }
}


