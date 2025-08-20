package me.javivi.pp.block.entity;

import me.javivi.pp.registry.ModBlockEntities;
import me.javivi.pp.wm.CustomVideoPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class ScreenBlockEntity extends BlockEntity {
    private @Nullable String videoUrl = null;
    private boolean loop = false;
    
    private transient @Nullable CustomVideoPlayer player = null;
    private transient int textureId = 0;
    private transient int videoWidth = 1;
    private transient int videoHeight = 1;
    private transient long lastLoopRestartMs = 0L;
    
    private @Nullable BlockPos screenMin = null;
    private @Nullable BlockPos screenMax = null;
    private transient boolean isMainScreen = false; 
    
    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCREEN, pos, state);
    }
    
    public void setScreenArea(BlockPos min, BlockPos max) {
        this.screenMin = min;
        this.screenMax = max;
        this.isMainScreen = getPos().equals(min); 
        markDirty();
        
        if (!isMainScreen && player != null) {
            stopPlayer();
        }
    }
    
    public void clearScreenArea() {
        this.screenMin = null;
        this.screenMax = null;
        this.isMainScreen = false;
        stopPlayer();
        markDirty();
    }
    
    public boolean hasScreenArea() {
        return screenMin != null && screenMax != null;
    }
    
    public boolean isMainScreen() {
        return isMainScreen;
    }
    
    public BlockPos getScreenMin() { return screenMin != null ? screenMin : getPos(); }
    public BlockPos getScreenMax() { return screenMax != null ? screenMax : getPos(); }
        
    public void setVideo(String url, boolean loop) {
        this.videoUrl = url;
        this.loop = loop;
        markDirty();
        
        if (isMainScreen) {
            startPlayer();
        }
    }
    
    public void stopVideo() {
        stopPlayer();
        this.videoUrl = null;
        this.loop = false;
        markDirty();
    }
    
    public @Nullable String getVideoUrl() { return videoUrl; }
    public boolean isLooping() { return loop; }
    
    public void clearUrl() { stopVideo(); }
    
    private void startPlayer() {
        if (videoUrl == null || world == null || !world.isClient) return;
        
        stopPlayer();
        
        try {
            var mc = MinecraftClient.getInstance();
            this.player = new CustomVideoPlayer(r -> mc.execute(r));
            
            // Intentar con la URL original
            try {
                this.player.start(new URI(videoUrl));
                return; // Si funciona, salir
            } catch (Throwable ignored) {}
            
            // Si falla, intentar con URL alternativa (sin parámetros extra)
            String cleanUrl = videoUrl.split("\\?")[0];
            if (!cleanUrl.equals(videoUrl)) {
                try {
                    this.player.start(new URI(cleanUrl));
                    return;
                } catch (Throwable ignored) {}
            }
            
            // Si todo falla, limpiar
            stopPlayer();
            
        } catch (Throwable ignored) {
            stopPlayer();
        }
    }
    
    public void stopPlayer() {
        if (player != null) {
            try { player.stop(); } catch (Throwable ignored) {}
            try { player.release(); } catch (Throwable ignored) {}
            player = null;
        }
        textureId = 0;
        videoWidth = 1;
        videoHeight = 1;
    }
    
    public void clientTick() {
        if (!isMainScreen || player == null || videoUrl == null) return;
        
        if (loop) {
            long now = System.currentTimeMillis();
            
            try {
                long duration = player.getDuration();
                long time = player.getTime();
                
                boolean shouldRestart = false;
                
                if (duration > 0) {
                    shouldRestart = time >= duration - 100;
                } else {
                    shouldRestart = player.isEnded() || 
                                  (textureId <= 0 && now - lastLoopRestartMs > 3000);
                }
                
                if (!shouldRestart && textureId <= 0 && now - lastLoopRestartMs > 5000) {
                    shouldRestart = true;
                }
                
                if (shouldRestart && (now - lastLoopRestartMs) >= 2000) {
                    lastLoopRestartMs = now;
                    
                    stopPlayer();
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    startPlayer();
                }
                
            } catch (Throwable ignored) {       
                if (now - lastLoopRestartMs >= 30000) {
                    lastLoopRestartMs = now;
                    stopPlayer();
                    startPlayer();
                }
            }
        }
    }
    
    public int getTextureId() {
        if (!isMainScreen || player == null) return 0;
        
        try {
            textureId = player.preRender();
            if (textureId > 0) {
                videoWidth = Math.max(1, player.width());
                videoHeight = Math.max(1, player.height());
            }
        } catch (Throwable ignored) {
            textureId = 0;
        }
        
        return textureId;
    }
    
    public int getVideoWidth() { return videoWidth; }
    public int getVideoHeight() { return videoHeight; }
    
    public @Nullable CustomVideoPlayer clientPlayer() { return player; }
    

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        if (videoUrl != null) nbt.putString("videoUrl", videoUrl);
        nbt.putBoolean("loop", loop);
        
        if (screenMin != null && screenMax != null) {
            nbt.putInt("minX", screenMin.getX());
            nbt.putInt("minY", screenMin.getY());
            nbt.putInt("minZ", screenMin.getZ());
            nbt.putInt("maxX", screenMax.getX());
            nbt.putInt("maxY", screenMax.getY());
            nbt.putInt("maxZ", screenMax.getZ());
        }
    }
    
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        if (nbt.contains("videoUrl")) videoUrl = nbt.getString("videoUrl");
        loop = nbt.getBoolean("loop");
        
        if (nbt.contains("minX")) {
            screenMin = new BlockPos(nbt.getInt("minX"), nbt.getInt("minY"), nbt.getInt("minZ"));
            screenMax = new BlockPos(nbt.getInt("maxX"), nbt.getInt("maxY"), nbt.getInt("maxZ"));
            isMainScreen = getPos().equals(screenMin);
        }
    }
    
    @Override
    public void markRemoved() {
        super.markRemoved();
        stopPlayer();
    }
}


