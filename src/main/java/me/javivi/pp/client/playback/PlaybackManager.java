package me.javivi.pp.client.playback;

import me.javivi.pp.play.AudioSession;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.ImageSession;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.client.gui.FreezeScreen;

public final class PlaybackManager {
    private static volatile @Nullable VideoSession videoSession;
    private static volatile @Nullable AudioSession audioSession;
    private static volatile @Nullable ImageSession imageSession;
    // Seguimiento de progreso para detectar fin cuando VLC no reporta duración/ended
    private static long lastVideoTimeMs = -1L;
    private static long lastVideoTimeWallMs = 0L;

    private PlaybackManager() {}

    public static void setVideoSession(@Nullable VideoSession session) {
        videoSession = session;
    }

    public static @Nullable VideoSession getVideoSession() { return videoSession; }

    public static @Nullable AudioSession getAudioSession() { return audioSession; }

    public static void setImageSession(@Nullable ImageSession session) {
        imageSession = session;
    }

    public static @Nullable ImageSession getImageSession() { return imageSession; }

    public static void stopImage() {
        try {
            if (imageSession != null) imageSession.stop();
        } catch (Throwable ignored) {}
        imageSession = null;
        try {
            var mcNow = MinecraftClient.getInstance();
            mcNow.execute(() -> {
                PixelplayClient.setImageSession(null);
                var overlay = PixelplayClient.getOverlay();
                if (overlay != null) {
                    overlay.setImageSession(null);
                    overlay.setEase(null);
                }
                if (mcNow.currentScreen instanceof FreezeScreen) {
                    mcNow.setScreen(null);
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void stopVideo() {
        try {
            if (videoSession != null) videoSession.stop();
        } catch (Throwable ignored) {}
        videoSession = null;
        lastVideoTimeMs = -1L;
        lastVideoTimeWallMs = 0L;
        try {
            var mcNow = MinecraftClient.getInstance();
            mcNow.execute(() -> {
                PixelplayClient.setVideoSession(null);
                var overlay = PixelplayClient.getOverlay();
                if (overlay != null) {
                    overlay.setSession(null);
                    overlay.setEase(null);
                }
                if (mcNow.currentScreen instanceof FreezeScreen) {
                    mcNow.setScreen(null);
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void setAudioSession(@Nullable AudioSession session) {
        audioSession = session;
    }

    public static void stopAudio() {
        if (audioSession != null) {
            try { audioSession.requestStop(); } catch (Throwable ignored) {}
        }
    }

    public static void tick(MinecraftClient mc) {
        if (audioSession != null) {
            try {
                audioSession.tick();
                if (audioSession.isStopped()) {
                    audioSession = null;
                }
            } catch (Throwable ignored) {}
        }
        if (videoSession != null) {
            try {
                var vp = videoSession.player();
                if (vp != null) {
                    long duration = vp.getDuration();
                    long time = vp.getTime();
                    // Asegurar que el outro arranca aunque haya FreezeScreen (el HUD puede no renderizar)
                    try {
                        videoSession.maybeStartOutro(duration, time, vp.isEnded());
                    } catch (Throwable ignored) {}
                    // Actualizar marcadores de avance
                    long nowWall = System.currentTimeMillis();
                    if (time != lastVideoTimeMs) {
                        lastVideoTimeMs = time;
                        lastVideoTimeWallMs = nowWall;
                    }
                    // Fin alcanzado según player o duración conocida
                    boolean endReached = vp.isEnded() || (duration > 0 && time >= duration - 50);
                    // Heurística: si no hay duración conocida y el tiempo no avanza durante >2s tras haber avanzado alguna vez, y no está reproduciendo
                    boolean progressedOnce = lastVideoTimeMs > 0L;
                    boolean stalled = progressedOnce && (nowWall - lastVideoTimeWallMs) > 2000L && !vp.isPlaying();
                    if (!endReached && duration <= 0 && stalled) {
                        // Forzar inicio de outro y considerar fin
                        try { videoSession.maybeStartOutro(0L, 0L, true); } catch (Throwable ignored) {}
                        endReached = true;
                    }
                    boolean outroFinished = videoSession != null && videoSession.hasOutro() && videoSession.isOutroFinished();
                    if (outroFinished || (endReached && (videoSession == null || !videoSession.hasOutro() || videoSession.isOutroFinished()))) {
                        try { videoSession.stop(); } catch (Throwable ignored) {}
                        videoSession = null;
                        lastVideoTimeMs = -1L;
                        lastVideoTimeWallMs = 0L;
                        try {
                            var mcNow = MinecraftClient.getInstance();
                            mcNow.execute(() -> {
                                PixelplayClient.setVideoSession(null);
                                var overlay = PixelplayClient.getOverlay();
                                if (overlay != null) {
                                    overlay.setSession(null);
                                    overlay.setEase(null);
                                }
                            });
                        } catch (Throwable ignored) {}
                        return;
                    }
                    if (!vp.isPlaying() && vp.isPaused() && vp.dimension() != null && vp.dimension().width > 1 && vp.dimension().height > 1) {
                        vp.play();
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (imageSession != null) {
            try {
                imageSession.maybeStartOutro();
                if (imageSession.shouldStop() || imageSession.isStopped() || imageSession.hasError()) {
                    try { imageSession.stop(); } catch (Throwable ignored) {}
                    imageSession = null;
                    try {
                        var mcNow = MinecraftClient.getInstance();
                        mcNow.execute(() -> {
                            PixelplayClient.setImageSession(null);
                            var overlay = PixelplayClient.getOverlay();
                            if (overlay != null) {
                                overlay.setImageSession(null);
                                overlay.setEase(null);
                            }
                        });
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }
}


