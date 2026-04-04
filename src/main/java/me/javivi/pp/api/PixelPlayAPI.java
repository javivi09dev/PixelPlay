package me.javivi.pp.api;

import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.play.AudioSession;
import me.javivi.pp.play.EaseSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.util.Easing;
import net.minecraft.client.MinecraftClient;

/**
 * Client-only API for local playback. Remote control for other players is provided by the PixelPlayFolia server plugin.
 */
public final class PixelPlayAPI {

    private PixelPlayAPI() {}

    public static boolean startVideoWithEase(String url, boolean freezeScreen,
                                             VideoSession.EaseColor easeColor,
                                             double introSeconds, double outroSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                VideoSession session = new VideoSession(
                    MinecraftClient.getInstance(),
                    url,
                    freezeScreen,
                    easeColor,
                    introSeconds,
                    outroSeconds,
                    Easing.Curve.EASE_IN_OUT_SINE
                );
                if (!session.hasError()) {
                    PixelplayClient.setVideoSession(session);
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startVideo(String url, boolean freezeScreen) {
        return startVideoWithEase(url, freezeScreen, VideoSession.EaseColor.BLACK, 0, 0);
    }

    public static boolean stopVideo() {
        try {
            MinecraftClient.getInstance().execute(PlaybackManager::stopVideo);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isVideoPlaying() {
        return PlaybackManager.getVideoSession() != null;
    }

    public static boolean startAudioWithEase(String url, double introSeconds, double outroSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                AudioSession session = new AudioSession(url, introSeconds, outroSeconds, Easing.Curve.EASE_IN_OUT_SINE);
                if (!session.hasError()) {
                    PlaybackManager.setAudioSession(session);
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startAudio(String url) {
        return startAudioWithEase(url, 0, 0);
    }

    public static boolean stopAudio() {
        try {
            MinecraftClient.getInstance().execute(PlaybackManager::stopAudio);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAudioPlaying() {
        return PlaybackManager.getAudioSession() != null;
    }

    public static boolean startEase(VideoSession.EaseColor easeColor,
                                    double introSeconds, double totalSeconds, double outroSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                boolean isWhite = easeColor == VideoSession.EaseColor.WHITE;
                EaseSession easeSession = new EaseSession(isWhite, introSeconds, totalSeconds, outroSeconds, Easing.Curve.EASE_IN_OUT_SINE);
                PixelplayClient.setEase(easeSession);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static float getVolumeMultiplier() {
        return me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier();
    }

    public static void setVolumeMultiplier(float multiplier) {
        me.javivi.pp.sound.MultimediaVolume.setMasterMultiplier(multiplier);
    }

    public static boolean startImageWithEase(String url, boolean freezeScreen,
                                             ImageSession.EaseColor easeColor,
                                             double introSeconds, double outroSeconds,
                                             double displayDurationSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                ImageSession session = new ImageSession(
                    MinecraftClient.getInstance(),
                    url,
                    freezeScreen,
                    easeColor,
                    introSeconds,
                    outroSeconds,
                    displayDurationSeconds,
                    Easing.Curve.EASE_IN_OUT_SINE
                );
                if (!session.hasError()) {
                    PixelplayClient.setImageSession(session);
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startImage(String url, boolean freezeScreen, double displayDurationSeconds) {
        return startImageWithEase(url, freezeScreen, ImageSession.EaseColor.BLACK, 0, 0, displayDurationSeconds);
    }

    public static boolean stopImage() {
        try {
            MinecraftClient.getInstance().execute(PlaybackManager::stopImage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isImageDisplaying() {
        return PlaybackManager.getImageSession() != null;
    }

    public static boolean isReady() {
        try {
            return MinecraftClient.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
