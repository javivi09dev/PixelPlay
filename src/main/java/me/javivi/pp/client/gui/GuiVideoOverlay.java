package me.javivi.pp.client.gui;

import me.javivi.pp.play.EaseSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.util.Easing;
import me.javivi.pp.wm.CustomVideoPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class GuiVideoOverlay {
    private static final float ASPECT_EPS = 0.0005f;

    private static final Identifier VIDEO_TEXTURE_ID = Identifier.of("pixelplay", "internal_wm_video_overlay");
    private static final Identifier IMAGE_TEXTURE_ID = Identifier.of("pixelplay", "internal_wm_image_overlay");

    private final MinecraftClient mc;
    private @Nullable VideoSession session;
    private @Nullable ImageSession imageSession;
    private @Nullable EaseSession ease;

    private int lastVideoTex = -1;
    private int lastVideoTw = -1;
    private int lastVideoTh = -1;

    private int lastImageTex = -1;
    private int lastImageTw = -1;
    private int lastImageTh = -1;

    private final int[] glSizeScratch = new int[2];

    public GuiVideoOverlay(MinecraftClient mc) {
        this.mc = mc;
    }

    public void setSession(@Nullable VideoSession session) {
        if (this.session != null && session == null) {
            unregisterVideoOverlayTexture();
        }
        this.session = session;
    }

    public void setImageSession(@Nullable ImageSession imageSession) {
        if (this.imageSession != null && imageSession == null) {
            unregisterImageOverlayTexture();
        }
        this.imageSession = imageSession;
    }

    public void setEase(@Nullable EaseSession ease) {
        this.ease = ease;
    }

    private void unregisterVideoOverlayTexture() {
        TextureManager tm = mc.getTextureManager();
        try {
            tm.destroyTexture(VIDEO_TEXTURE_ID);
        } catch (Throwable ignored) {}
        lastVideoTex = -1;
        lastVideoTw = -1;
        lastVideoTh = -1;
    }

    private void unregisterImageOverlayTexture() {
        TextureManager tm = mc.getTextureManager();
        try {
            tm.destroyTexture(IMAGE_TEXTURE_ID);
        } catch (Throwable ignored) {}
        lastImageTex = -1;
        lastImageTw = -1;
        lastImageTh = -1;
    }

    public void renderEaseOnly(DrawContext context, float tickProgress) {
        if (ease != null) {
            float a = ease.alpha();
            if (a > 0.001f) {
                fillFade(context, a, ease.isWhite() ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK);
            }
            if (ease.finished()) {
                ease = null;
            }
        }
    }

    public void render(DrawContext context, float tickProgress) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (session != null) {
            if (session.isStopped()) {
                unregisterVideoOverlayTexture();
                this.session = null;
            } else if (session.hasError()) {
                session.stop();
                unregisterVideoOverlayTexture();
                this.session = null;
            } else {
                CustomVideoPlayer player = session.player();
                if (player != null) {
                    try {
                        player.setVolumeMultiplier(me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier());
                    } catch (Throwable ignored) {
                    }

                    int tex;
                    try {
                        tex = player.preRender();
                    } catch (Throwable t) {
                        session.stop();
                        unregisterVideoOverlayTexture();
                        this.session = null;
                        tex = 0;
                    }
                    if (tex > 0) {
                        session.markFirstFrame();

                        int vw = Math.max(1, player.width());
                        int vh = Math.max(1, player.height());
                        if (vw <= 1 || vh <= 1) {
                            vw = sw;
                            vh = sh;
                        }

                        GlTextureSize.getBound2DSize(tex, glSizeScratch);
                        int tw = glSizeScratch[0] > 0 ? glSizeScratch[0] : vw;
                        int th = glSizeScratch[1] > 0 ? glSizeScratch[1] : vh;

                        int rw = Math.min(vw, tw);
                        int rh = Math.min(vh, th);

                        BlitRect rect = fitCover(sw, sh, vw, vh);

                        float videoAlpha = session.introAlphaFromFirstFrame();
                        long dur = player.getDuration();
                        long now = player.getTime();
                        boolean playerEnded = player.isEnded();
                        if (session.hasOutro()) {
                            session.maybeStartOutro(dur, now, playerEnded);
                            float outro = session.outroProgressMonotonic();
                            videoAlpha *= (1.0f - Easing.clamp01(outro));
                        }
                        int argb = argb(Easing.clamp01(videoAlpha), 255, 255, 255);

                        if (tex != lastVideoTex || tw != lastVideoTw || th != lastVideoTh) {
                            mc.getTextureManager().registerTexture(VIDEO_TEXTURE_ID, new ExternalTexture(tex, tw, th));
                            lastVideoTex = tex;
                            lastVideoTw = tw;
                            lastVideoTh = th;
                        }

                        context.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            VIDEO_TEXTURE_ID,
                            rect.dx,
                            rect.dy,
                            0f,
                            0f,
                            rect.dw,
                            rect.dh,
                            rw,
                            rh,
                            tw,
                            th,
                            argb
                        );

                        float introMask = session.maskIntroAlphaNow();
                        if (introMask > 0.001f) {
                            fillFade(context, introMask, session.easeColor());
                        }
                    }
                }
            }
        }

        if (imageSession != null) {
            if (imageSession.isStopped()) {
                unregisterImageOverlayTexture();
                this.imageSession = null;
            } else if (imageSession.hasError()) {
                imageSession.stop();
                unregisterImageOverlayTexture();
                this.imageSession = null;
            } else if (imageSession.isLoaded()) {
                imageSession.markFirstFrame();
                imageSession.updateDimensions();

                int imgSw = mc.getWindow().getScaledWidth();
                int imgSh = mc.getWindow().getScaledHeight();
                int iw = imageSession.getImageWidth();
                int ih = imageSession.getImageHeight();
                if (iw <= 1 || ih <= 1) {
                    iw = imgSw;
                    ih = imgSh;
                }

                int texId = imageSession.getTextureIdFromRenderer();
                if (texId > 0) {
                    GlTextureSize.getBound2DSize(texId, glSizeScratch);
                    int tw = glSizeScratch[0] > 0 ? glSizeScratch[0] : iw;
                    int th = glSizeScratch[1] > 0 ? glSizeScratch[1] : ih;
                    int rw = Math.min(iw, tw);
                    int rh = Math.min(ih, th);

                    BlitRect rect = fitCover(imgSw, imgSh, iw, ih);

                    float imageAlpha = imageSession.introAlphaFromFirstFrame();
                    if (imageSession.hasOutro()) {
                        imageSession.maybeStartOutro();
                        float outro = imageSession.outroProgressMonotonic();
                        imageAlpha *= (1.0f - Easing.clamp01(outro));
                    }
                    int argb = argb(Easing.clamp01(imageAlpha), 255, 255, 255);

                    if (texId != lastImageTex || tw != lastImageTw || th != lastImageTh) {
                        mc.getTextureManager().registerTexture(IMAGE_TEXTURE_ID, new ExternalTexture(texId, tw, th));
                        lastImageTex = texId;
                        lastImageTw = tw;
                        lastImageTh = th;
                    }

                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        IMAGE_TEXTURE_ID,
                        rect.dx,
                        rect.dy,
                        0f,
                        0f,
                        rect.dw,
                        rect.dh,
                        rw,
                        rh,
                        tw,
                        th,
                        argb
                    );
                } else if (imageSession.textureId() != null) {
                    BlitRect rect = fitCover(imgSw, imgSh, iw, ih);
                    float imageAlpha = imageSession.introAlphaFromFirstFrame();
                    if (imageSession.hasOutro()) {
                        imageSession.maybeStartOutro();
                        float outro = imageSession.outroProgressMonotonic();
                        imageAlpha *= (1.0f - Easing.clamp01(outro));
                    }
                    int argb = argb(Easing.clamp01(imageAlpha), 255, 255, 255);
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        imageSession.textureId(),
                        rect.dx,
                        rect.dy,
                        0f,
                        0f,
                        rect.dw,
                        rect.dh,
                        256,
                        256,
                        argb
                    );
                }

                float introMask = imageSession.maskIntroAlphaNow();
                if (introMask > 0.001f) {
                    fillFade(context, introMask, imageSession.easeColor());
                }
            }
        }

        if (ease != null) {
            float a = ease.alpha();
            if (a > 0.001f) {
                fillFade(context, a, ease.isWhite() ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK);
            }
            if (ease.finished()) {
                ease = null;
            }
        }
    }

    /**
     * Escala para cubrir toda la ventana (pantalla completa); recorta lo que sobresale por los lados o arriba/abajo.
     */
    private static BlitRect fitCover(int sw, int sh, int cw, int ch) {
        float screenAspect = (float) sw / (float) sh;
        float contentAspect = (float) cw / (float) ch;
        int dw;
        int dh;
        if (contentAspect > screenAspect + ASPECT_EPS) {
            dh = sh;
            dw = Math.max(1, Math.round(sh * contentAspect));
        } else if (contentAspect < screenAspect - ASPECT_EPS) {
            dw = sw;
            dh = Math.max(1, Math.round((float) sw / contentAspect));
        } else {
            dw = sw;
            dh = sh;
        }
        int dx = (sw - dw) / 2;
        int dy = (sh - dh) / 2;
        return new BlitRect(dx, dy, dw, dh);
    }

    private record BlitRect(int dx, int dy, int dw, int dh) {}

    private static int argb(float a, int r, int g, int b) {
        int ai = Math.round(Easing.clamp01(a) * 255.0f);
        return (ai << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    private void fillFade(DrawContext ctx, float alpha, VideoSession.EaseColor color) {
        int a = Math.round(Easing.clamp01(alpha) * 255.0f);
        int rgb = color == VideoSession.EaseColor.BLACK ? 0x000000 : 0xFFFFFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), argb);
    }

    private void fillFade(DrawContext ctx, float alpha, ImageSession.EaseColor color) {
        int a = Math.round(Easing.clamp01(alpha) * 255.0f);
        int rgb = color == ImageSession.EaseColor.BLACK ? 0x000000 : 0xFFFFFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), argb);
    }
}
