package me.javivi.pp.block.entity;

import me.javivi.pp.registry.ModBlockEntities;
import me.javivi.pp.wm.CustomVideoPlayer;
import me.javivi.pp.network.payload.ScreenVideoPayload;
import me.javivi.pp.network.payload.ScreenVolumePayload;
import me.javivi.pp.network.payload.ScreenPausePayload;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class ScreenBlockEntity extends BlockEntity {
    private @Nullable String videoUrl = null;
    private boolean loop = false;
    private float volume = 1.0f; // Volumen específico de la pantalla (0.0 a 1.0)
    private boolean paused = false; // Estado de pausa
    
    private transient @Nullable CustomVideoPlayer player = null;
    private transient @Nullable CustomVideoPlayer nextPlayer = null; // Pre-loaded next player for seamless loop
    private transient int textureId = 0;
    private transient int videoWidth = 1;
    private transient int videoHeight = 1;
    private transient long lastLoopRestartMs = 0L;
    private transient long lastVideoTimeMs = -1L;
    private transient long lastVideoTimeWallMs = 0L;
    private transient boolean preparingNext = false;
    
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
        
        // Sync to all clients in multiplayer (only from server side)
        if (world != null && !world.isClient && world instanceof ServerWorld serverWorld) {
            ScreenVideoPayload payload = new ScreenVideoPayload(getPos(), url, loop, screenMin, screenMax);
            // Send to all players in the world (they will filter by chunk loading)
            serverWorld.getPlayers().forEach(player -> {
                // Only send if player is within reasonable distance
                double dist = player.getBlockPos().getSquaredDistance(getPos());
                if (dist <= 128 * 128) { // 128 blocks = 8 chunks
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
        
        // Start player on client side
        if (world != null && world.isClient && isMainScreen) {
            startPlayer();
        }
    }
    
    // Internal method to set video without syncing (used when receiving from server)
    public void setVideoFromServer(String url, boolean loop) {
        this.videoUrl = url;
        this.loop = loop;
        markDirty();
        
        // Start player on client side (no network sync needed, we're already on client)
        if (world != null && world.isClient && isMainScreen) {
            startPlayer();
        }
    }
    
    public void stopVideo() {
        stopPlayer();
        this.videoUrl = null;
        this.loop = false;
        markDirty();
        
        // Sync to all clients in multiplayer
        if (world != null && !world.isClient && world instanceof ServerWorld serverWorld) {
            ScreenVideoPayload payload = new ScreenVideoPayload(getPos(), null, false, screenMin, screenMax);
            // Send to all players in the world (they will filter by chunk loading)
            serverWorld.getPlayers().forEach(player -> {
                // Only send if player is within reasonable distance
                double dist = player.getBlockPos().getSquaredDistance(getPos());
                if (dist <= 128 * 128) { // 128 blocks = 8 chunks
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
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
                // Aplicar volumen y pausa después de iniciar
                applyVolume();
                if (paused) {
                    applyPause();
                }
                return; // Si funciona, salir
            } catch (Throwable ignored) {}
            
            // Si falla, intentar con URL alternativa (sin parámetros extra)
            String cleanUrl = videoUrl.split("\\?")[0];
            if (!cleanUrl.equals(videoUrl)) {
                try {
                    this.player.start(new URI(cleanUrl));
                    // Aplicar volumen y pausa después de iniciar
                    applyVolume();
                    if (paused) {
                        applyPause();
                    }
                    return;
                } catch (Throwable ignored) {}
            }
            
            // Si todo falla, limpiar
            stopPlayer();
            
        } catch (Throwable ignored) {
            stopPlayer();
        }
    }
    
    private void prepareNextPlayer() {
        if (videoUrl == null || world == null || !world.isClient || preparingNext) return;
        
        try {
            preparingNext = true;
            var mc = MinecraftClient.getInstance();
            this.nextPlayer = new CustomVideoPlayer(r -> mc.execute(r));
            
            // Intentar con la URL original
            try {
                this.nextPlayer.start(new URI(videoUrl));
                return; // Si funciona, salir
            } catch (Throwable ignored) {}
            
            // Si falla, intentar con URL alternativa (sin parámetros extra)
            String cleanUrl = videoUrl.split("\\?")[0];
            if (!cleanUrl.equals(videoUrl)) {
                try {
                    this.nextPlayer.start(new URI(cleanUrl));
                    return;
                } catch (Throwable ignored) {}
            }
            
            // Si todo falla, limpiar
            if (nextPlayer != null) {
                try { nextPlayer.stop(); } catch (Throwable ignored) {}
                try { nextPlayer.release(); } catch (Throwable ignored) {}
                nextPlayer = null;
            }
            preparingNext = false;
            
        } catch (Throwable t) {
            if (nextPlayer != null) {
                try { nextPlayer.stop(); } catch (Throwable ignored) {}
                try { nextPlayer.release(); } catch (Throwable ignored) {}
                nextPlayer = null;
            }
            preparingNext = false;
        }
    }
    
    public void stopPlayer() {
        if (player != null) {
            try { player.stop(); } catch (Throwable ignored) {}
            try { player.release(); } catch (Throwable ignored) {}
            player = null;
        }
        if (nextPlayer != null) {
            try { nextPlayer.stop(); } catch (Throwable ignored) {}
            try { nextPlayer.release(); } catch (Throwable ignored) {}
            nextPlayer = null;
        }
        textureId = 0;
        videoWidth = 1;
        videoHeight = 1;
        lastVideoTimeMs = -1L;
        lastVideoTimeWallMs = 0L;
        preparingNext = false;
    }
    
    public void clientTick() {
        if (!isMainScreen || player == null || videoUrl == null) return;
        
        if (loop) {
            long now = System.currentTimeMillis();
            
            try {
                // Check if video has ended
                boolean ended = player.isEnded();
                
                // Get current time and duration
                long duration = player.getDuration();
                long time = player.getTime();
                boolean isPlaying = player.isPlaying();
                
                // Update time tracking
                if (time != lastVideoTimeMs && time > 0) {
                    lastVideoTimeMs = time;
                    lastVideoTimeWallMs = now;
                }
                
                // Check if we should prepare next player (when near end, ~300ms before)
                if (duration > 0 && time > 0) {
                    long timeLeft = duration - time;
                    // Start preparing next player when we're 300ms from the end
                    if (timeLeft <= 300 && timeLeft > 100 && nextPlayer == null && !preparingNext) {
                        // Prepare next player in background
                        MinecraftClient.getInstance().execute(() -> {
                            prepareNextPlayer();
                        });
                    }
                }
                
                // Check if we should restart
                boolean shouldRestart = false;
                
                // Method 1: Video explicitly ended
                if (ended) {
                    shouldRestart = true;
                }
                // Method 2: We're at or past the end (within 50ms tolerance) OR next player is ready
                else if (duration > 0 && time > 0) {
                    long timeLeft = duration - time;
                    // Restart if we're very close to the end OR if next player is ready
                    if (timeLeft <= 50 || (nextPlayer != null && timeLeft <= 200)) {
                        shouldRestart = true;
                    }
                }
                // Method 3: Video stalled (not playing and time hasn't advanced in 2+ seconds)
                else if (lastVideoTimeMs > 0 && !isPlaying) {
                    long timeSinceLastUpdate = now - lastVideoTimeWallMs;
                    if (timeSinceLastUpdate > 2000) {
                        shouldRestart = true;
                    }
                }
                // Method 4: No duration info but video stopped playing and hasn't updated
                else if (duration <= 0 && !isPlaying && lastVideoTimeMs > 0) {
                    long timeSinceLastUpdate = now - lastVideoTimeWallMs;
                    if (timeSinceLastUpdate > 3000) {
                        shouldRestart = true;
                    }
                }
                
                // Restart if needed (with minimal cooldown for seamless transition)
                if (shouldRestart && (now - lastLoopRestartMs) >= 100) {
                    lastLoopRestartMs = now;
                    
                    // If we have a pre-loaded next player, use it immediately
                    if (nextPlayer != null) {
                        // Swap players instantly for seamless transition
                        CustomVideoPlayer oldPlayer = player;
                        player = nextPlayer;
                        nextPlayer = null;
                        preparingNext = false;
                        
                        // Clean up old player
                        if (oldPlayer != null) {
                            try { oldPlayer.stop(); } catch (Throwable ignored) {}
                            try { oldPlayer.release(); } catch (Throwable ignored) {}
                        }
                        
                        // Reset tracking
                        lastVideoTimeMs = -1L;
                        lastVideoTimeWallMs = 0L;
                    } else {
                        // No pre-loaded player, do normal restart (should be rare)
                        lastVideoTimeMs = -1L;
                        lastVideoTimeWallMs = 0L;
                        
                        // Stop player first
                        if (player != null) {
                            try { player.stop(); } catch (Throwable ignored) {}
                            try { player.release(); } catch (Throwable ignored) {}
                            player = null;
                        }
                        textureId = 0;
                        
                        // Start new player immediately
                        MinecraftClient.getInstance().execute(() -> {
                            if (videoUrl != null && loop && isMainScreen) {
                                startPlayer();
                            }
                        });
                    }
                }
                
            } catch (Throwable t) {
                // If there's an error and it's been a while, try to restart
                if ((now - lastLoopRestartMs) >= 5000) {
                    lastLoopRestartMs = now;
                    lastVideoTimeMs = -1L;
                    lastVideoTimeWallMs = 0L;
                    stopPlayer();
                    MinecraftClient.getInstance().execute(() -> {
                        if (videoUrl != null && loop && isMainScreen) {
                            startPlayer();
                        }
                    });
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
    
    public float getVolume() { return volume; }
    public boolean isPaused() { return paused; }
    
    public void setVolume(float newVolume) {
        this.volume = Math.max(0f, Math.min(1f, newVolume));
        markDirty();
        
        // Sync to all clients in multiplayer (only from server side)
        if (world != null && !world.isClient && world instanceof ServerWorld serverWorld) {
            ScreenVolumePayload payload = new ScreenVolumePayload(getPos(), volume);
            serverWorld.getPlayers().forEach(player -> {
                double dist = player.getBlockPos().getSquaredDistance(getPos());
                if (dist <= 128 * 128) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
        
        // Apply volume on client side
        if (world != null && world.isClient && isMainScreen && player != null) {
            applyVolume();
        }
    }
    
    public void setPaused(boolean newPaused) {
        this.paused = newPaused;
        markDirty();
        
        // Sync to all clients in multiplayer (only from server side)
        if (world != null && !world.isClient && world instanceof ServerWorld serverWorld) {
            ScreenPausePayload payload = new ScreenPausePayload(getPos(), paused);
            serverWorld.getPlayers().forEach(player -> {
                double dist = player.getBlockPos().getSquaredDistance(getPos());
                if (dist <= 128 * 128) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
        
        // Apply pause on client side
        if (world != null && world.isClient && isMainScreen && player != null) {
            applyPause();
        }
    }
    
    // Internal methods to set volume/pause without syncing (used when receiving from server)
    public void setVolumeFromServer(float newVolume) {
        this.volume = Math.max(0f, Math.min(1f, newVolume));
        markDirty();
        
        // Apply volume on client side
        if (world != null && world.isClient && isMainScreen && player != null) {
            applyVolume();
        }
    }
    
    public void setPausedFromServer(boolean newPaused) {
        this.paused = newPaused;
        markDirty();
        
        // Apply pause on client side
        if (world != null && world.isClient && isMainScreen && player != null) {
            applyPause();
        }
    }
    
    private void applyVolume() {
        if (player == null) return;
        try {
            // Aplicar volumen específico de la pantalla (independiente del volumen multimedia)
            // El volumen multimedia se aplica en el renderer para la atenuación por distancia
            player.setVolumeMultiplier(volume);
        } catch (Throwable ignored) {}
    }
    
    private void applyPause() {
        if (player == null) return;
        try {
            if (paused) {
                if (player.isPlaying() && player.raw() != null) {
                    player.raw().mediaPlayer().controls().setPause(true);
                }
            } else {
                if (player.isPaused() && player.raw() != null) {
                    player.raw().mediaPlayer().controls().setPause(false);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        if (videoUrl != null) nbt.putString("videoUrl", videoUrl);
        nbt.putBoolean("loop", loop);
        nbt.putFloat("volume", volume); // Guardar volumen
        nbt.putBoolean("paused", paused); // Guardar estado de pausa
        
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
        if (nbt.contains("volume")) volume = nbt.getFloat("volume");
        if (nbt.contains("paused")) paused = nbt.getBoolean("paused");
        
        if (nbt.contains("minX")) {
            screenMin = new BlockPos(nbt.getInt("minX"), nbt.getInt("minY"), nbt.getInt("minZ"));
            screenMax = new BlockPos(nbt.getInt("maxX"), nbt.getInt("maxY"), nbt.getInt("maxZ"));
            isMainScreen = getPos().equals(screenMin);
        }
        
        // Start player on client when loading from NBT (for reconnection)
        if (world != null && world.isClient && isMainScreen && videoUrl != null && !videoUrl.isEmpty()) {
            // Schedule start on next tick to ensure world is fully loaded
            MinecraftClient.getInstance().execute(() -> {
                if (this.videoUrl != null && !this.videoUrl.isEmpty() && this.isMainScreen) {
                    startPlayer();
                    // Apply saved volume and pause state
                    applyVolume();
                    applyPause();
                }
            });
        }
    }
    
    @Override
    public void markRemoved() {
        super.markRemoved();
        stopPlayer();
    }
}


