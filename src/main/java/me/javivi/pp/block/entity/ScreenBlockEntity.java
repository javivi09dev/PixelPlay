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
    private @Nullable CustomVideoPlayer player = null;
    private int texId = 0;
    private int width = 1;
    private int height = 1;

    // Región conectada en el plano del bloque (sólo cliente)
    private transient @Nullable BlockPos regionMin = null;
    private transient @Nullable BlockPos regionMax = null;
    private transient boolean controller = true;
    private transient long regionComputedAtMs = 0L;
    private boolean loop = false;
    private @Nullable BlockPos explicitMin = null;
    private @Nullable BlockPos explicitMax = null;

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCREEN, pos, state);
    }

    public void setUrl(String url) {
        this.videoUrl = url;
        // Propagar a toda la región en cliente para que cualquier controlador futuro tenga la URL
        if (world != null && world.isClient) propagateUrlToRegion(url);
        // Iniciar sólo si este BE es controlador
        if (world != null && world.isClient) {
            ensureRegionComputed();
            if (controller) startPlayer(); else stopPlayer();
        }
        markDirty();
    }

    public void setLoop(boolean loop) { this.loop = loop; }
    public boolean isLoop() { return loop; }

    public @Nullable String getUrl() { return videoUrl; }

    public void clearUrl() { this.videoUrl = null; }

    private void startPlayer() {
        stopPlayer();
        if (videoUrl == null || world == null || world.isClient) return;
    }

    public @Nullable CustomVideoPlayer clientPlayer() { return player; }

    public int textureId() { return texId; }

    public int videoWidth() { return width; }

    public int videoHeight() { return height; }

    public void clientEnsureStarted() {
        ensureRegionComputed();
        if (!controller) { if (player != null) stopPlayer(); return; }
        if (player != null) return;
        if (videoUrl == null) return;
        try {
            var mc = MinecraftClient.getInstance();
            this.player = new CustomVideoPlayer(r -> mc.execute(r));
            this.player.start(new URI(videoUrl));
        } catch (Throwable ignored) {}
    }

    public int clientPreRender() {
        if (player == null) return 0;
        int tex = 0;
        try {
            tex = player.preRender();
            this.texId = tex;
            this.width = Math.max(1, player.width());
            this.height = Math.max(1, player.height());
            if (loop && player.getDuration() > 0) {
                long dur = player.getDuration();
                long t = player.getTime();
                if (player.isEnded() || t >= dur - 50) {
                    try { if (videoUrl != null) player.start(new URI(videoUrl)); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return tex;
    }

    public void stopPlayer() {
        try { if (player != null) player.stop(); } catch (Throwable ignored) {}
        try { if (player != null) player.release(); } catch (Throwable ignored) {}
        player = null;
        texId = 0;
    }

    // ===== Región conectada (cliente) =====
    public void ensureRegionComputed() {
        if (world == null || !world.isClient) return;
        long now = System.currentTimeMillis();
        if (regionMin != null && (now - regionComputedAtMs) < 500L) return;
        // Recalcular siempre que haya cambios visibles: si un vecino ya no es el mismo bloque, el BFS lo reflejará

        regionComputedAtMs = now;
        BlockPos start = getPos();
        var state = getCachedState();
        if (state == null) {
            regionMin = start; regionMax = start; controller = true; return;
        }
        var block = state.getBlock();
        var facing = state.get(me.javivi.pp.block.ScreenBlock.FACING);

        // Si hay región explícita, úsala
        if (explicitMin != null && explicitMax != null) {
            regionMin = explicitMin;
            regionMax = explicitMax;
            boolean isCtrl;
            if (facing == net.minecraft.util.math.Direction.NORTH || facing == net.minecraft.util.math.Direction.SOUTH) {
                isCtrl = (start.getY() == explicitMin.getY()) && (start.getX() == explicitMin.getX());
            } else {
                isCtrl = (start.getY() == explicitMin.getY()) && (start.getZ() == explicitMin.getZ());
            }
            controller = isCtrl;
            if (!controller && player != null) stopPlayer();
            return;
        }

        int minX = start.getX(), maxX = start.getX();
        int minY = start.getY(), maxY = start.getY();
        int minZ = start.getZ(), maxZ = start.getZ();

        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> vis = new java.util.HashSet<>();
        q.add(start);
        vis.add(start);
        int visited = 0;
        while (!q.isEmpty() && visited < 256) {
            BlockPos p = q.pollFirst();
            visited++;
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());

            // vecinos en el plano según facing
            BlockPos[] neighbors;
            switch (facing) {
                case NORTH, SOUTH -> neighbors = new BlockPos[]{ p.east(), p.west(), p.up(), p.down() };
                case EAST, WEST -> neighbors = new BlockPos[]{ p.north(), p.south(), p.up(), p.down() };
                default -> neighbors = new BlockPos[]{ p.east(), p.west(), p.up(), p.down() };
            }
            for (BlockPos n : neighbors) {
                if (vis.contains(n)) continue;
                var ns = world.getBlockState(n);
                if (ns != null && ns.getBlock() == block && ns.contains(me.javivi.pp.block.ScreenBlock.FACING) && ns.get(me.javivi.pp.block.ScreenBlock.FACING) == facing) {
                    vis.add(n);
                    q.add(n);
                }
            }
        }

        // Guardar región y determinar controlador (el de menor Y, luego menor X/Z en eje horizontal)
        regionMin = new BlockPos(minX, minY, minZ);
        regionMax = new BlockPos(maxX, maxY, maxZ);
        boolean isCtrl;
        if (facing == net.minecraft.util.math.Direction.NORTH || facing == net.minecraft.util.math.Direction.SOUTH) {
            isCtrl = (start.getY() == minY) && (start.getX() == minX);
        } else {
            isCtrl = (start.getY() == minY) && (start.getZ() == minZ);
        }
        controller = isCtrl;
        // Si deja de ser controlador, detener su player para evitar audio duplicado
        if (!controller && player != null) stopPlayer();
    }

    public boolean isController() { ensureRegionComputed(); return controller; }
    public BlockPos regionMin() { ensureRegionComputed(); return regionMin != null ? regionMin : getPos(); }
    public BlockPos regionMax() { ensureRegionComputed(); return regionMax != null ? regionMax : getPos(); }

    private void propagateUrlToRegion(String url) {
        if (world == null || !world.isClient) return;
        ensureRegionComputed();
        var state = getCachedState();
        if (state == null) return;
        var block = state.getBlock();
        var facing = state.get(me.javivi.pp.block.ScreenBlock.FACING);
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> vis = new java.util.HashSet<>();
        q.add(getPos()); vis.add(getPos());
        int visited = 0;
        while (!q.isEmpty() && visited < 512) {
            BlockPos p = q.pollFirst();
            visited++;
            var be = world.getBlockEntity(p);
            if (be instanceof ScreenBlockEntity s) {
                s.videoUrl = url;
                s.loop = this.loop;
                if (!s.controller && s.player != null) s.stopPlayer();
            }
            BlockPos[] neighbors;
            switch (facing) {
                case NORTH, SOUTH -> neighbors = new BlockPos[]{ p.east(), p.west(), p.up(), p.down() };
                case EAST, WEST -> neighbors = new BlockPos[]{ p.north(), p.south(), p.up(), p.down() };
                default -> neighbors = new BlockPos[]{ p.east(), p.west(), p.up(), p.down() };
            }
            for (BlockPos n : neighbors) {
                if (!vis.add(n)) continue;
                var ns = world.getBlockState(n);
                if (ns != null && ns.getBlock() == block && ns.contains(me.javivi.pp.block.ScreenBlock.FACING) && ns.get(me.javivi.pp.block.ScreenBlock.FACING) == facing) {
                    q.add(n);
                }
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (videoUrl != null) nbt.putString("url", videoUrl);
        nbt.putBoolean("loop", loop);
        if (explicitMin != null && explicitMax != null) {
            nbt.putInt("eminx", explicitMin.getX());
            nbt.putInt("eminy", explicitMin.getY());
            nbt.putInt("eminz", explicitMin.getZ());
            nbt.putInt("emaxx", explicitMax.getX());
            nbt.putInt("emaxy", explicitMax.getY());
            nbt.putInt("emaxz", explicitMax.getZ());
            nbt.putBoolean("hasExplicit", true);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("url")) videoUrl = nbt.getString("url");
        loop = nbt.getBoolean("loop");
        if (nbt.getBoolean("hasExplicit")) {
            explicitMin = new BlockPos(nbt.getInt("eminx"), nbt.getInt("eminy"), nbt.getInt("eminz"));
            explicitMax = new BlockPos(nbt.getInt("emaxx"), nbt.getInt("emaxy"), nbt.getInt("emaxz"));
        } else {
            explicitMin = explicitMax = null;
        }
    }

    public void setExplicitRegion(BlockPos min, BlockPos max) {
        this.explicitMin = min;
        this.explicitMax = max;
        this.regionMin = null; // forzar recomputación
        this.regionMax = null;
        this.regionComputedAtMs = 0L;
    }

    public void clearExplicitRegion() {
        this.explicitMin = null;
        this.explicitMax = null;
        this.regionComputedAtMs = 0L;
    }
}


