package me.javivi.pp.wm;

import org.watermedia.api.media.players.MediaPlayer;

import java.awt.*;

/**
 * Adaptador del {@link MediaPlayer} de WaterMedia 3.x (FFmpeg) a la API que usa PixelPlay (overlay, freeze, volumen).
 */
public final class CustomVideoPlayer {

    private final MediaPlayer media;

    public CustomVideoPlayer(MediaPlayer media) {
        this.media = media;
    }

    public int preRender() {
        if (media == null) return 0;
        int w = media.width();
        int h = media.height();
        if (w <= 0 || h <= 0) return 0;
        int tex = media.texture();
        return tex > 0 ? tex : 0;
    }

    public int width() {
        return media != null ? Math.max(1, media.width()) : 1;
    }

    public int height() {
        return media != null ? Math.max(1, media.height()) : 1;
    }

    public long getDuration() {
        return media != null ? media.duration() : -1L;
    }

    public long getTime() {
        return media != null ? media.time() : -1L;
    }

    public boolean isEnded() {
        return media != null && media.ended();
    }

    public boolean isPlaying() {
        return media != null && media.playing();
    }

    public boolean isPaused() {
        return media != null && media.paused();
    }

    public void play() {
        if (media != null) media.resume();
    }

    public void setVolumeMultiplier(float multiplier) {
        if (media == null || !media.withAudio()) return;
        multiplier = Math.max(0f, Math.min(1f, multiplier));
        media.volume(Math.round(multiplier * 100f));
    }

    public boolean hasUploadedOnce() {
        return media != null && media.width() > 1 && media.height() > 1 && media.texture() > 0;
    }

    public int texture() {
        return media != null ? media.texture() : 0;
    }

    public Dimension dimension() {
        if (media == null) return null;
        int w = media.width();
        int h = media.height();
        if (w > 1 && h > 1) return new Dimension(w, h);
        return null;
    }

    public void stop() {
        if (media != null) media.stop();
    }

    public void release() {
        if (media != null) media.release();
    }
}
