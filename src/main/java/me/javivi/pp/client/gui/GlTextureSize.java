package me.javivi.pp.client.gui;

import org.lwjgl.opengl.GL11;

/**
 * Dimensiones almacenadas en la GPU para una textura 2D (pueden ser mayores que el vídeo visible por alineación del decodificador).
 */
public final class GlTextureSize {

    private GlTextureSize() {}

    public static void getBound2DSize(int glTextureId, int[] outTwTh) {
        if (glTextureId <= 0) {
            outTwTh[0] = 0;
            outTwTh[1] = 0;
            return;
        }
        int prev = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
        outTwTh[0] = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        outTwTh[1] = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prev);
    }
}
