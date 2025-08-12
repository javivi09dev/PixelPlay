package me.javivi.pp.wm;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL12;
import org.watermedia.api.player.videolan.BasePlayer;
import org.watermedia.api.render.RenderAPI;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;
import org.watermedia.videolan4j.player.base.MediaPlayer;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferCleanupCallback;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferFormat;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferFormatCallback;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.RenderCallback;
import org.watermedia.videolan4j.tools.Chroma;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.watermedia.WaterMedia.LOGGER;

public class CustomVideoPlayer extends BasePlayer implements RenderCallback, BufferFormatCallback, BufferCleanupCallback {
    private static final Marker IT = MarkerManager.getMarker("CustomVideoPlayer");

    private int width = 1;
    private int height = 1;
    private int size = width * height * 4;
    private boolean refresh = false;
    private boolean first = true;
    private volatile boolean uploadedOnce = false;
    private final int texture;
    private final Semaphore semaphore = new Semaphore(1);
    private final Executor renderExecutor;
    private ByteBuffer[] buffers;

    public CustomVideoPlayer(Executor renderExecutor) { this(null, renderExecutor); }

    public CustomVideoPlayer(MediaPlayerFactory factory, Executor renderExecutor) {
        super();
        this.texture = RenderAPI.createTexture();
        this.renderExecutor = renderExecutor;
        this.init(factory, this, this, this);
        if (raw() == null) {
            RenderAPI.deleteTexture(texture);
        } else {
            raw().mediaPlayer().videoSurface().getVideoSurface().setSemaphore(semaphore);
        }
    }

    @Override
    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
        this.refresh = true;
    }

    @Override
    public void allocatedBuffers(ByteBuffer[] buffers) {
        this.buffers = buffers;
        this.first = true;
    }

    @Override
    public void cleanupBuffers(ByteBuffer[] buffers) {
        this.buffers = null;
    }

    @Override
    public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
        this.width = sourceWidth;
        this.height = sourceHeight;
        this.size = sourceWidth * sourceHeight * 4;
        this.first = true;
        return new BufferFormat(Chroma.RV32, sourceWidth, sourceHeight);
    }

    public int size() { return size; }
    public int width() { return width; }
    public int height() { return height; }

    public int preRender() {
        RenderAPI.bindTexture(this.texture);
        try {
            if (semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                if (refresh && buffers != null && buffers.length > 0) {
                    RenderAPI.uploadBuffer(buffers[0], texture, GL12.GL_BGRA, width, height, first);
                    first = false;
                    refresh = false;
                    uploadedOnce = true;
                }
                semaphore.release();
            } else {
                LOGGER.error(IT, "{} took more than 1 second to synchronize with native threads", this, new InterruptedByTimeoutException());
                if (first) throw new IllegalStateException("Cannot handle interruption");
                this.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        RenderAPI.bindTexture(RenderAPI.NONE);
        return texture;
    }

    /** Control de volumen por slider Multimedia */
    public void applyGlobalVolume() {
        try {
            setVolumeMultiplier(me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier());
        } catch (Throwable ignored) {}
    }

    public void setVolumeMultiplier(float multiplier) {
        multiplier = Math.max(0f, Math.min(1f, multiplier));
        if (raw() != null) {
            try {
                int base = 100;
                raw().mediaPlayer().audio().setVolume(Math.round(base * multiplier));
            } catch (Throwable ignored) {}
        }
    }

    public boolean hasUploadedOnce() { return uploadedOnce; }

    public int texture() { return texture; }

    public Dimension dimension() {
        if (width > 1 && height > 1) return new Dimension(width, height);
        if (raw() == null) return null;
        return raw().mediaPlayer().video().videoDimension();
    }

    @Override
    public void release() {
        renderExecutor.execute(() -> RenderAPI.deleteTexture(texture));
        super.release();
    }
}


