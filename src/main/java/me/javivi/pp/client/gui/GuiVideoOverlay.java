package me.javivi.pp.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.EaseSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.util.Easing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import me.javivi.pp.wm.CustomVideoPlayer;


public final class GuiVideoOverlay {
    private final MinecraftClient mc;
    private @Nullable VideoSession session;
    private @Nullable ImageSession imageSession;
    private @Nullable EaseSession ease;
    private final net.minecraft.util.Identifier runtimeId = net.minecraft.util.Identifier.of("pixelplay", "overlay_runtime");
    private int lastRegisteredTex = -1;

    public GuiVideoOverlay(MinecraftClient mc) {
        this.mc = mc;
    }

    public void setSession(@Nullable VideoSession session) {
        this.session = session;
    }

    public void setImageSession(@Nullable ImageSession imageSession) {
        this.imageSession = imageSession;
    }

    public void setEase(@Nullable EaseSession ease) { this.ease = ease; }

    // Dibuja SOLO la capa de ease, para usar desde TAIL del HUD
    public void renderEaseOnly(DrawContext context, float tickDelta) {
        if (ease != null) {
            float a = ease.alpha();
            if (a > 0.001f) fillFade(context, a, ease.isWhite() ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK);
            if (ease.finished()) ease = null;
        }
    }

    public void render(DrawContext context, float tickDelta) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // 1) Vídeo si hay sesión activa
        if (session != null) {
            if (session.isStopped()) { this.session = null; }
            else if (session.hasError()) { session.stop(); this.session = null; }
            else {
                CustomVideoPlayer player = session.player();
                if (player != null) {
                    try { player.setVolumeMultiplier(me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier()); } catch (Throwable ignored) {}

                    int tex;
                    try {
                        tex = player.preRender();
                    } catch (Throwable t) {
                        session.stop();
                        this.session = null;
                        tex = 0;
                    }
                    if (tex > 0) {
                        session.markFirstFrame();

                        int vw = Math.max(1, player.width());
                        int vh = Math.max(1, player.height());
                        if (vw <= 1 || vh <= 1) { vw = sw; vh = sh; }

                        float screenAspect = (float) sw / (float) sh;
                        float videoAspect = (float) vw / (float) vh;
                        int dw = sw;
                        int dh = sh;
                        if (videoAspect > screenAspect) { dh = sh; dw = Math.round(sh * videoAspect); }
                        else if (videoAspect < screenAspect) { dw = sw; dh = Math.round(sw / videoAspect); }
                        int dx = (sw - dw) / 2;
                        int dy = (sh - dh) / 2;

                        RenderSystem.disableScissor();
                        RenderSystem.disableCull();
                        RenderSystem.depthMask(false);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableDepthTest();
                        RenderSystem.colorMask(true, true, true, true);
                        RenderSystem.setShader(GameRenderer::getPositionTexProgram);

                        float videoAlpha = session.introAlphaFromFirstFrame();
                        long dur = player.getDuration();
                        long now = player.getTime();
                        boolean playerEnded = player.isEnded();
                        if (session.hasOutro()) {
                            session.maybeStartOutro(dur, now, playerEnded);
                            float outro = session.outroProgressMonotonic();
                            videoAlpha *= (1.0f - Easing.clamp01(outro));
                        }
                        RenderSystem.setShaderColor(1f, 1f, 1f, Easing.clamp01(videoAlpha));
                        if (tex != lastRegisteredTex) {
                            mc.getTextureManager().registerTexture(runtimeId, new ExternalTexture(tex));
                            lastRegisteredTex = tex;
                        }
                        RenderSystem.setShaderTexture(0, runtimeId);
                        RenderSystem.disableDepthTest();
                        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
                        Tessellator tess = Tessellator.getInstance();
                        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                        buf.vertex(matrix, dx, dy, 0).texture(0f, 0f);
                        buf.vertex(matrix, dx + dw, dy, 0).texture(1f, 0f);
                        buf.vertex(matrix, dx + dw, dy + dh, 0).texture(1f, 1f);
                        buf.vertex(matrix, dx, dy + dh, 0).texture(0f, 1f);
                        BufferRenderer.drawWithGlobalProgram(buf.end());
                        RenderSystem.depthMask(true);
                        float introMask = session.maskIntroAlphaNow();
                        if (introMask > 0.001f) fillFade(context, introMask, session.easeColor());
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                        RenderSystem.enableDepthTest();
                        RenderSystem.disableBlend();
                    }
                }
            }
        }

        // 1.5) Imagen si hay sesión activa
        if (imageSession != null) {
            if (imageSession.isStopped()) { this.imageSession = null; }
            else if (imageSession.hasError()) { imageSession.stop(); this.imageSession = null; }
            else if (imageSession.isLoaded()) {
                imageSession.markFirstFrame();
                imageSession.updateDimensions(); // Update dimensions if available

                int imgSw = mc.getWindow().getScaledWidth();
                int imgSh = mc.getWindow().getScaledHeight();
                int iw = imageSession.getImageWidth();
                int ih = imageSession.getImageHeight();
                if (iw <= 1 || ih <= 1) { iw = imgSw; ih = imgSh; }

                float screenAspect = (float) imgSw / (float) imgSh;
                float imageAspect = (float) iw / (float) ih;
                int dw = imgSw;
                int dh = imgSh;
                if (imageAspect > screenAspect) { dh = imgSh; dw = Math.round(imgSh * imageAspect); }
                else if (imageAspect < screenAspect) { dw = imgSw; dh = Math.round(imgSw / imageAspect); }
                int dx = (imgSw - dw) / 2;
                int dy = (imgSh - dh) / 2;

                RenderSystem.disableScissor();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);

                float imageAlpha = imageSession.introAlphaFromFirstFrame();
                if (imageSession.hasOutro()) {
                    imageSession.maybeStartOutro();
                    float outro = imageSession.outroProgressMonotonic();
                    imageAlpha *= (1.0f - Easing.clamp01(outro));
                }
                RenderSystem.setShaderColor(1f, 1f, 1f, Easing.clamp01(imageAlpha));
                
                // Use texture from ImageRenderer (supports animated GIFs)
                // Get texture ID each frame for animated images
                int texId = imageSession.getTextureIdFromRenderer();
                if (texId > 0) {
                    // Register texture if it changed (for animated GIFs)
                    if (texId != lastRegisteredTex) {
                        mc.getTextureManager().registerTexture(runtimeId, new ExternalTexture(texId));
                        lastRegisteredTex = texId;
                    }
                    RenderSystem.setShaderTexture(0, runtimeId);
                } else if (imageSession.textureId() != null) {
                    // Fallback to registered texture if renderer texture not available
                    RenderSystem.setShaderTexture(0, imageSession.textureId());
                } else {
                    // No texture available, skip rendering
                    return;
                }
                RenderSystem.disableDepthTest();
                Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                buf.vertex(matrix, dx, dy, 0).texture(0f, 0f);
                buf.vertex(matrix, dx + dw, dy, 0).texture(1f, 0f);
                buf.vertex(matrix, dx + dw, dy + dh, 0).texture(1f, 1f);
                buf.vertex(matrix, dx, dy + dh, 0).texture(0f, 1f);
                BufferRenderer.drawWithGlobalProgram(buf.end());
                RenderSystem.depthMask(true);
                float introMask = imageSession.maskIntroAlphaNow();
                if (introMask > 0.001f) fillFade(context, introMask, imageSession.easeColor());
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }
        }

        // 2) Ease SIEMPRE dibuja, incluso sin vídeo, y al final (prioridad máxima)
        if (ease != null) {
            float a = ease.alpha();
            if (a > 0.001f) fillFade(context, a, ease.isWhite() ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK);
            if (ease.finished()) ease = null;
        }
    }

    private void fillFade(DrawContext ctx, float alpha, VideoSession.EaseColor color) {
        int a = Math.round(Easing.clamp01(alpha) * 255.0f);
        int rgb = color == VideoSession.EaseColor.BLACK ? 0x000000 : 0xFFFFFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        // Usar la capa de overlay del GUI y z muy frontal para asegurar prioridad máxima
        ctx.fill(RenderLayer.getGuiOverlay(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), -100, argb);
    }

    private void fillFade(DrawContext ctx, float alpha, ImageSession.EaseColor color) {
        int a = Math.round(Easing.clamp01(alpha) * 255.0f);
        int rgb = color == ImageSession.EaseColor.BLACK ? 0x000000 : 0xFFFFFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        // Usar la capa de overlay del GUI y z muy frontal para asegurar prioridad máxima
        ctx.fill(RenderLayer.getGuiOverlay(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), -100, argb);
    }
}


