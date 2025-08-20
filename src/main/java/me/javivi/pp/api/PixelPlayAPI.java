package me.javivi.pp.api;

import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.AudioSession;
import me.javivi.pp.util.Easing;
import net.minecraft.client.MinecraftClient;

/**
 * PixelPlay API 
 * 
 * 
 * You can use it for you own mods, but it's supposed to work for Kindly Klan's events.
 * 
 * 
 * 
 */
public final class PixelPlayAPI {
    
    private PixelPlayAPI() {} // Utility class
    
    // ===== VIDEO PLAYBACK =====
    
    /**
     * Starts a video with fade-in and fade-out effects
     * 
     * @param url Video URL (YouTube, direct MP4, etc.)
     * @param freezeScreen Whether to freeze player input during playback
     * @param easeColor Color of the fade effect (WHITE or BLACK)
     * @param introSeconds Duration of fade-in effect in seconds
     * @param outroSeconds Duration of fade-out effect in seconds
     * @return true if video started successfully, false otherwise
     */
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
                PixelplayClient.setVideoSession(session);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Starts a simple video without effects
     * 
     * @param url Video URL
     * @param freezeScreen Whether to freeze player input
     * @return true if video started successfully, false otherwise
     */
    public static boolean startVideo(String url, boolean freezeScreen) {
        return startVideoWithEase(url, freezeScreen, VideoSession.EaseColor.BLACK, 0, 0);
    }
    
    /**
     * Stops the currently playing video
     * 
     * @return true if video was stopped, false if no video was playing
     */
    public static boolean stopVideo() {
        try {
            MinecraftClient.getInstance().execute(() -> {
                PlaybackManager.stopVideo();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if a video is currently playing
     * 
     * @return true if video is active, false otherwise
     */
    public static boolean isVideoPlaying() {
        return PlaybackManager.getVideoSession() != null;
    }
    
    // ===== AUDIO PLAYBACK =====
    
    /**
     * Starts audio playback with fade effects
     * 
     * @param url Audio URL
     * @param introSeconds Fade-in duration in seconds
     * @param outroSeconds Fade-out duration in seconds
     * @return true if audio started successfully, false otherwise
     */
    public static boolean startAudioWithEase(String url, double introSeconds, double outroSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                AudioSession session = new AudioSession(url, introSeconds, outroSeconds, Easing.Curve.EASE_IN_OUT_SINE);
                PlaybackManager.setAudioSession(session);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Starts simple audio playback
     * 
     * @param url Audio URL
     * @return true if audio started successfully, false otherwise
     */
    public static boolean startAudio(String url) {
        return startAudioWithEase(url, 0, 0);
    }
    
    /**
     * Stops the currently playing audio
     * 
     * @return true if audio was stopped, false if no audio was playing
     */
    public static boolean stopAudio() {
        try {
            MinecraftClient.getInstance().execute(() -> {
                PlaybackManager.stopAudio();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if audio is currently playing
     * 
     * @return true if audio is active, false otherwise
     */
    public static boolean isAudioPlaying() {
        return PlaybackManager.getAudioSession() != null;
    }
    
    // ===== EASE EFFECTS =====
    
    /**
     * Starts a fade effect (screen transition)
     * 
     * @param easeColor Color of the fade (WHITE or BLACK)
     * @param introSeconds Fade-in duration
     * @param totalSeconds Total effect duration
     * @param outroSeconds Fade-out duration
     * @return true if effect started successfully, false otherwise
     */
    public static boolean startEase(VideoSession.EaseColor easeColor, 
                                   double introSeconds, double totalSeconds, double outroSeconds) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                boolean isWhite = easeColor == VideoSession.EaseColor.WHITE;
                me.javivi.pp.play.EaseSession easeSession = new me.javivi.pp.play.EaseSession(
                    isWhite, introSeconds, totalSeconds, outroSeconds, Easing.Curve.EASE_IN_OUT_SINE
                );
                PixelplayClient.setEase(easeSession);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ===== UTILITY METHODS :) =====
    
    /**
     * Gets the current volume multiplier for multimedia
     * 
     * @return Volume multiplier (0.0 to 1.0)
     */
    public static float getVolumeMultiplier() {
        return me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier();
    }
    
    /**
     * Sets the volume multiplier for multimedia
     * 
     * @param multiplier Volume multiplier (0.0 to 1.0)
     */
    public static void setVolumeMultiplier(float multiplier) {
        me.javivi.pp.sound.MultimediaVolume.setMasterMultiplier(multiplier);
    }
    
    /**
     * Checks if PixelPlay is available and ready
     * 
     * @return true if API is ready, false otherwise
     */
    public static boolean isReady() {
        try {
            return MinecraftClient.getInstance() != null && 
                   MinecraftClient.getInstance().world != null;
        } catch (Exception e) {
            return false;
        }
    }
}
