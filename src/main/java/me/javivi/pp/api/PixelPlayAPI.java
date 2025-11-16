package me.javivi.pp.api;

import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.AudioSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.util.Easing;
import me.javivi.pp.network.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.UUID;

/**
 * PixelPlay API 
 * 
 * This API provides methods to control video and audio playback
 * from other mods. All methods are thread-safe and can be called from
 * any thread.
 * 
 * You can use it for your own mods, but it's supposed to work for Kindly Klan's events.
 * 
 * @since 1.3
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
     * Starts a video with fade-in and fade-out effects for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Video URL (YouTube, direct MP4, etc.)
     * @param freezeScreen Whether to freeze player input during playback
     * @param easeColor Color of the fade effect (WHITE or BLACK)
     * @param introSeconds Duration of fade-in effect in seconds
     * @param outroSeconds Duration of fade-out effect in seconds
     * @return true if video started successfully, false otherwise
     */
    public static boolean startVideoWithEase(List<UUID> targetPlayers, String url, boolean freezeScreen, 
                                           VideoSession.EaseColor easeColor, 
                                           double introSeconds, double outroSeconds) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return startVideoWithEase(url, freezeScreen, easeColor, introSeconds, outroSeconds);
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                ClientPlayNetworking.send(new StartVideoC2SPayload(
                    playerId,
                    url,
                    freezeScreen,
                    easeColor == VideoSession.EaseColor.WHITE,
                    introSeconds,
                    outroSeconds
                ));
            }
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
     * Starts a simple video without effects for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Video URL
     * @param freezeScreen Whether to freeze player input
     * @return true if video started successfully, false otherwise
     */
    public static boolean startVideo(List<UUID> targetPlayers, String url, boolean freezeScreen) {
        return startVideoWithEase(targetPlayers, url, freezeScreen, VideoSession.EaseColor.BLACK, 0, 0);
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
     * Stops video for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @return true if stop command sent successfully, false otherwise
     */
    public static boolean stopVideo(List<UUID> targetPlayers) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return stopVideo();
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                ClientPlayNetworking.send(new StopVideoC2SPayload(playerId));
            }
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
     * Starts audio playback with fade effects for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Audio URL
     * @param introSeconds Fade-in duration in seconds
     * @param outroSeconds Fade-out duration in seconds
     * @return true if audio started successfully, false otherwise
     */
    public static boolean startAudioWithEase(List<UUID> targetPlayers, String url, double introSeconds, double outroSeconds) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return startAudioWithEase(url, introSeconds, outroSeconds);
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                ClientPlayNetworking.send(new StartAudioC2SPayload(playerId, url, introSeconds, outroSeconds));
            }
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
     * Starts simple audio playback for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Audio URL
     * @return true if audio started successfully, false otherwise
     */
    public static boolean startAudio(List<UUID> targetPlayers, String url) {
        return startAudioWithEase(targetPlayers, url, 0, 0);
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
     * Stops audio for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @return true if stop command sent successfully, false otherwise
     */
    public static boolean stopAudio(List<UUID> targetPlayers) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return stopAudio();
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                ClientPlayNetworking.send(new StopAudioC2SPayload(playerId));
            }
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
    
    /**
     * Starts a fade effect for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param easeColor Color of the fade (WHITE or BLACK)
     * @param introSeconds Fade-in duration
     * @param totalSeconds Total effect duration
     * @param outroSeconds Fade-out duration
     * @return true if effect started successfully, false otherwise
     */
    public static boolean startEase(List<UUID> targetPlayers, VideoSession.EaseColor easeColor, 
                                   double introSeconds, double totalSeconds, double outroSeconds) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return startEase(easeColor, introSeconds, totalSeconds, outroSeconds);
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                boolean isWhite = easeColor == VideoSession.EaseColor.WHITE;
                ClientPlayNetworking.send(new StartEaseC2SPayload(playerId, isWhite, introSeconds, totalSeconds, outroSeconds));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ===== UTILITY METHODS =====
    
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
    
    // ===== IMAGE PLAYBACK =====
    
    /**
     * Starts displaying an image with fade-in and fade-out effects
     * 
     * @param url Image URL (supports images and GIFs)
     * @param freezeScreen Whether to freeze player input during display
     * @param easeColor Color of the fade effect (WHITE or BLACK)
     * @param introSeconds Duration of fade-in effect in seconds
     * @param outroSeconds Duration of fade-out effect in seconds
     * @param displayDurationSeconds Duration to display the image in seconds (0 for infinite)
     * @return true if image started successfully, false otherwise
     */
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
                PixelplayClient.setImageSession(session);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Starts displaying an image with fade-in and fade-out effects for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Image URL (supports images and GIFs)
     * @param freezeScreen Whether to freeze player input during display
     * @param easeColor Color of the fade effect (WHITE or BLACK)
     * @param introSeconds Duration of fade-in effect in seconds
     * @param outroSeconds Duration of fade-out effect in seconds
     * @param displayDurationSeconds Duration to display the image in seconds (0 for infinite)
     * @return true if image started successfully, false otherwise
     */
    public static boolean startImageWithEase(List<UUID> targetPlayers, String url, boolean freezeScreen, 
                                           ImageSession.EaseColor easeColor, 
                                           double introSeconds, double outroSeconds,
                                           double displayDurationSeconds) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return startImageWithEase(url, freezeScreen, easeColor, introSeconds, outroSeconds, displayDurationSeconds);
        }
        
        try {
            // Send to multiple players
            for (UUID playerId : targetPlayers) {
                ClientPlayNetworking.send(new StartImageC2SPayload(
                    playerId,
                    url,
                    freezeScreen,
                    easeColor == ImageSession.EaseColor.WHITE,
                    introSeconds,
                    outroSeconds,
                    displayDurationSeconds
                ));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Starts displaying an image without effects
     * 
     * @param url Image URL
     * @param freezeScreen Whether to freeze player input
     * @param displayDurationSeconds Duration to display the image in seconds (0 for infinite)
     * @return true if image started successfully, false otherwise
     */
    public static boolean startImage(String url, boolean freezeScreen, double displayDurationSeconds) {
        return startImageWithEase(url, freezeScreen, ImageSession.EaseColor.BLACK, 0, 0, displayDurationSeconds);
    }
    
    /**
     * Starts displaying an image without effects for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @param url Image URL
     * @param freezeScreen Whether to freeze player input
     * @param displayDurationSeconds Duration to display the image in seconds (0 for infinite)
     * @return true if image started successfully, false otherwise
     */
    public static boolean startImage(List<UUID> targetPlayers, String url, boolean freezeScreen, double displayDurationSeconds) {
        return startImageWithEase(targetPlayers, url, freezeScreen, ImageSession.EaseColor.BLACK, 0, 0, displayDurationSeconds);
    }
    
    /**
     * Stops the currently displaying image
     * 
     * @return true if image was stopped, false if no image was displaying
     */
    public static boolean stopImage() {
        try {
            MinecraftClient.getInstance().execute(() -> {
                PlaybackManager.stopImage();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Stops image for specific players
     * 
     * @param targetPlayers List of player UUIDs (null or empty for local player)
     * @return true if stop command sent successfully, false otherwise
     */
    public static boolean stopImage(List<UUID> targetPlayers) {
        if (targetPlayers == null || targetPlayers.isEmpty()) {
            // Local player
            return stopImage();
        }
        
        // Note: StopImageC2SPayload would need to be created if needed
        // For now, we'll just return false for remote players
        return false;
    }
    
    /**
     * Checks if an image is currently displaying
     * 
     * @return true if image is active, false otherwise
     */
    public static boolean isImageDisplaying() {
        return PlaybackManager.getImageSession() != null;
    }
    
    // ===== UTILITY METHODS =====
    
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
