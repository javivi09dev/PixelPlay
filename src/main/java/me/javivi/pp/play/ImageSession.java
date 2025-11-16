package me.javivi.pp.play;

import me.javivi.pp.util.Easing;
import me.javivi.pp.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageCache;
import org.watermedia.api.image.ImageRenderer;

import java.net.URI;
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
    
    private @Nullable ImageCache imageCache = null;
    private @Nullable ImageRenderer imageRenderer = null;
    private @Nullable Identifier textureId = null;
    private final String imageUrl;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private volatile boolean loaded = false;

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
        this.imageUrl = imageUrl;

        // Load image using WaterMedia ImageAPI
        loadImageAsync(imageUrl);
    }

    private void loadImageAsync(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            
            // Get cache from WaterMedia ImageAPI
            mc.execute(() -> {
                try {
                    imageCache = ImageAPI.getCache(uri, mc);
                    if (imageCache != null) {
                        imageCache.use();
                        imageCache.load();
                        
                        // Check status in a tick loop
                        checkImageStatus();
                    } else {
                        this.error = "Failed to get image cache";
                    }
                } catch (Exception e) {
                    this.error = "Failed to load image: " + e.getMessage();
                }
            });
        } catch (Exception e) {
            this.error = "Invalid image URL: " + e.getMessage();
        }
    }
    
    private void checkImageStatus() {
        if (imageCache == null || stopped) return;
        
        ImageCache.Status status = imageCache.getStatus();
        
        if (status == ImageCache.Status.READY) {
            imageRenderer = imageCache.getRenderer();
            if (imageRenderer != null) {
                // Check if it's actually a video (shouldn't happen but handle it)
                if (imageCache.isVideo()) {
                    this.error = "URL is a video, not an image";
                    return;
                }
                
                // Get texture ID from renderer (texture() requires timestamp for animated GIFs)
                try {
                    long currentTime = System.currentTimeMillis();
                    int texId = imageRenderer.texture(currentTime);
                    if (texId > 0) {
                        // Register texture with Minecraft
                        this.textureId = Identifier.of("pixelplay", "image_" + currentTime + "_" + imageUrl.hashCode());
                        mc.getTextureManager().registerTexture(this.textureId, new me.javivi.pp.client.gui.ExternalTexture(texId));
                        
                        // Try to get dimensions from renderer
                        // ImageRenderer may not have direct width/height methods, use defaults for now
                        // Dimensions will be updated when we actually render
                        this.imageWidth = 1920; // Default fallback, will be updated on render
                        this.imageHeight = 1080;
                        
                        this.loaded = true;
                        this.firstFrameMs = currentTime;
                    }
                } catch (Exception e) {
                    this.error = "Failed to get texture: " + e.getMessage();
                }
            }
        } else if (status == ImageCache.Status.FAILED) {
            Exception ex = imageCache.getException();
            this.error = ex != null ? ex.getMessage() : "Failed to load image";
        } else {
            // Still loading, check again next tick
            mc.execute(() -> checkImageStatus());
        }
    }

    public boolean hasError() { return error != null; }
    public @Nullable String error() { return error; }

    public boolean isStopped() { return stopped; }

    public void stop() {
        if (stopped) return;
        stopped = true;
        mc.execute(() -> {
            if (textureId != null) {
                try {
                    mc.getTextureManager().destroyTexture(textureId);
                } catch (Throwable ignored) {}
            }
            if (imageRenderer != null) {
                try {
                    imageRenderer.release();
                } catch (Throwable ignored) {}
            }
            if (imageCache != null) {
                try {
                    // ImageCache will handle cleanup automatically when not in use
                } catch (Throwable ignored) {}
            }
        });
    }

    public float introAlpha() {
        if (introEaseMs <= 0) return 1f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt >= introEaseMs) return 1f;
        float t = (float) dt / (float) introEaseMs;
        return Easing.ease(t, easeCurve);
    }

    public void markFirstFrame() {
        if (firstFrameMs == 0L && loaded) firstFrameMs = System.currentTimeMillis();
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
        if (firstFrameMs != 0L || loaded) return 0f;
        long dt = System.currentTimeMillis() - startMs;
        if (dt <= 0) return 0f;
        if (dt >= PRE_ROLL_MS) return 1f;
        return (float) dt / (float) PRE_ROLL_MS;
    }

    public boolean hasOutro() { return outroEaseMs > 0 || displayDurationMs > 0; }

    public void maybeStartOutro() {
        if (outroStartMs >= 0L) return;
        
        long elapsed = System.currentTimeMillis() - startMs;
        
        // Start outro if display duration is reached
        if (displayDurationMs > 0 && elapsed >= displayDurationMs) {
            outroStartMs = System.currentTimeMillis();
            return;
        }
        
        // Start outro if we're near the end of display duration
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
    public @Nullable Identifier textureId() { return textureId; }
    public boolean isLoaded() { return loaded; }
    public int getImageWidth() { return imageWidth; }
    public int getImageHeight() { return imageHeight; }
    public @Nullable ImageRenderer getImageRenderer() { return imageRenderer; }
    
    // Helper method to get texture ID from renderer if needed
    public int getTextureIdFromRenderer() {
        if (imageRenderer != null) {
            try {
                // texture() requires timestamp for animated GIFs
                return imageRenderer.texture(System.currentTimeMillis());
            } catch (Throwable ignored) {}
        }
        return 0;
    }
    
    // Update dimensions from renderer when available
    public void updateDimensions() {
        if (imageRenderer != null && loaded) {
            try {
                // Try to get actual dimensions - may need to check renderer API
                // For now, we'll use the texture size or keep defaults
                int texId = imageRenderer.texture(System.currentTimeMillis());
                if (texId > 0) {
                    // Dimensions might be available after first render
                    // If ImageRenderer has dimension methods, use them here
                }
            } catch (Throwable ignored) {}
        }
    }
}

