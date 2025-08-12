package me.javivi.pp.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.util.Easing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL12;
import me.javivi.pp.wm.CustomVideoPlayer;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;

import java.net.URI;

public final class VideoScreen extends Screen {
    private final String url;
    private final VideoSession.EaseColor easeColor;
    private final long introMs;
    private final Easing.Curve curve;

    private final CustomVideoPlayer player;
    private boolean started;
    private boolean playedAfterDim;
    private long startTime;
    private final net.minecraft.util.Identifier runtimeId = net.minecraft.util.Identifier.of("pixelplay", "video_runtime");
    private int lastRegisteredTex = -1;

    public VideoScreen(String url,
                       boolean freezeScreen,
                       boolean whiteEase,
                       double introSeconds,
                       double outroSeconds,
                       Easing.Curve curve) {
        super(Text.empty());
        this.url = url;
        this.easeColor = whiteEase ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK;
        this.introMs = (long) Math.max(0, introSeconds * 1000.0);
        this.curve = curve != null ? curve : Easing.Curve.EASE_IN_OUT_SINE; 
        String[] vlcArgs = new String[] {
                "--no-quiet",
                "--cr-average=5000",
                "--swscale-mode=0",
                "--network-caching=1500",
                "--live-caching=1500",
                "--file-caching=1500",
                "--aout", "directsound,waveout,mmdevice",
                "--vout", "none",
                "--avcodec-skip-idct=4",
                "--avcodec-fast",
                "--avcodec-hw", "none",
                "--no-metadata-network-access",
                "--no-file-logging",
                "--http-reconnect"
        };
        MediaPlayerFactory factory = new MediaPlayerFactory(vlcArgs);
        this.player = new CustomVideoPlayer(factory, r -> MinecraftClient.getInstance().execute(r));
        this.started = false;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        try {
            if (this.introMs > 0) this.player.startPaused(new URI(url));
            else this.player.start(new URI(url));
            this.started = true;
            this.startTime = System.currentTimeMillis();
        } catch (Exception e) {
            closeNow();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!started) return;
        int texId;
        try { texId = player.preRender(); } catch (Throwable t) { texId = -1; }
        if (texId <= 0) {
            RenderSystem.enableBlend();
            context.fill(0, 0, this.width, this.height, 0xFF000000);
            RenderSystem.disableBlend();
            applyFades(context);
            return;
        }

        int dw = this.width, dh = this.height, dx = 0, dy = 0;
        int vw = Math.max(1, player.width());
        int vh = Math.max(1, player.height());
        if (vw > 1 && vh > 1) {
            float screenAspect = (float) this.width / (float) this.height;
            float videoAspect = (float) vw / (float) vh;
            if (videoAspect > screenAspect) { dh = Math.round(this.width / videoAspect); dy = (this.height - dh) / 2; }
            else if (videoAspect < screenAspect) { dw = Math.round(this.height * videoAspect); dx = (this.width - dw) / 2; }
        }

        if (vw > 1 && vh > 1 && !playedAfterDim && this.introMs > 0) {
            try { player.play(); playedAfterDim = true; } catch (Throwable ignored) {}
        }

        RenderSystem.disableBlend();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        if (texId != lastRegisteredTex) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(runtimeId, new ExternalTexture(texId));
            lastRegisteredTex = texId;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, runtimeId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buf.vertex(matrix, dx,       dy,       0).texture(0f, 0f);
        buf.vertex(matrix, dx + dw,  dy,       0).texture(1f, 0f);
        buf.vertex(matrix, dx + dw,  dy + dh,  0).texture(1f, 1f);
        buf.vertex(matrix, dx,       dy + dh,  0).texture(0f, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyFades(context);
        RenderSystem.disableBlend();

        if (player.isEnded() || player.isBroken()) closeNow();

    }

    private void applyFades(DrawContext context) {
        long now = System.currentTimeMillis();
        if (introMs > 0) {
            long dt = now - startTime;
            if (dt < introMs) {
                float t = (float) dt / (float) introMs;
                float alpha = 1.0f - Easing.ease(t, curve);
                fillFade(context, alpha);
            }
        }
    }

    private void fillFade(DrawContext ctx, float alpha) {
        int a = Math.round(Easing.clamp01(alpha) * 255.0f);
        int rgb = (easeColor == VideoSession.EaseColor.WHITE) ? 0xFFFFFF : 0x000000;
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        ctx.fill(0, 0, this.width, this.height, argb);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
    @Override public void close() { closeNow(); }
    private void closeNow() {
        try { player.stop(); } catch (Throwable ignored) {}
        try { player.release(); } catch (Throwable ignored) {}
        if (this.client != null) this.client.setScreen(null);
    }
}


