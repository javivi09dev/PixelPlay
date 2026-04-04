package me.javivi.pp.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
/**
 * Wraps an existing OpenGL texture id (from WaterMedia) as a {@link GpuTexture} for the 1.21.11 renderer.
 * Does not delete the GL name on close — the native player owns it.
 */
public final class ExternalTexture extends AbstractTexture {

    public ExternalTexture(int glId, int width, int height) {
        GlTexture gl = new NonClosingGlTexture(glId, width, height);
        this.glTexture = gl;
        this.glTextureView = new GlTextureView(gl, 0, gl.getMipLevels());
    }

    @Override
    public void close() {
        // Do not free GlTexture / GL id; WaterMedia manages the texture object.
    }

    private static final class NonClosingGlTexture extends GlTexture {
        NonClosingGlTexture(int glId, int width, int height) {
            super(GpuTexture.USAGE_TEXTURE_BINDING, "pixelplay:watermedia", TextureFormat.RGBA8, width, height, 0, 1, glId);
        }

        @Override
        public void close() {
            // no-op: external GL texture
        }
    }
}
