package me.javivi.pp.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.javivi.pp.block.ScreenBlock;
import me.javivi.pp.block.entity.ScreenBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    public ScreenBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ScreenBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!be.isMainScreen()) return;
        
        int textureId = be.getTextureId();
        if (textureId <= 0) return;
        
        Direction facing = be.getCachedState().get(ScreenBlock.FACING);
        BlockPos min = be.getScreenMin();
        BlockPos max = be.getScreenMax();
        
        float screenWidth = max.getX() - min.getX() + 1;
        float screenHeight = max.getY() - min.getY() + 1;
        
        float centerX = (min.getX() + max.getX()) / 2.0f - be.getPos().getX();
        float centerY = (min.getY() + max.getY()) / 2.0f - be.getPos().getY();
        float centerZ = 0f;
        
        if (facing == Direction.EAST || facing == Direction.WEST) {
            centerZ = (min.getZ() + max.getZ()) / 2.0f - be.getPos().getZ();
        }
        
        matrices.push();
        matrices.translate(0.5 + centerX, 0.5 + centerY, 0.5 + centerZ);
        
        switch (facing) {
            case NORTH -> {}
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f));
        }
        
        matrices.translate(0.0, 0.0, -0.501);
        
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        
        var pos = be.getPos();
        Identifier textureIdentifier = Identifier.of("pixelplay", "screen_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        MinecraftClient.getInstance().getTextureManager().registerTexture(textureIdentifier, new me.javivi.pp.client.gui.ExternalTexture(textureId));
        RenderSystem.setShaderTexture(0, textureIdentifier);
        
        try {
            var player = be.clientPlayer();
            if (player != null) {
                var playerEnt = MinecraftClient.getInstance().player;
                if (playerEnt != null) {
                    double cx = (min.getX() + max.getX()) / 2.0 + 0.5;
                    double cy = (min.getY() + max.getY()) / 2.0 + 0.5;
                    double cz = facing == Direction.NORTH || facing == Direction.SOUTH ? 
                        be.getPos().getZ() + 0.5 : (min.getZ() + max.getZ()) / 2.0 + 0.5;
                    
                    double dx = playerEnt.getX() - cx;
                    double dy = playerEnt.getEyeY() - cy;
                    double dz = playerEnt.getZ() - cz;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    double maxDist = 32.0;
                    float atten = (float)Math.max(0.0, 1.0 - dist / maxDist);
                    float slider = me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier();
                    player.setVolumeMultiplier(slider * atten);
                }
            }
        } catch (Throwable ignored) {}
        
        float videoAspect = (float)be.getVideoWidth() / (float)be.getVideoHeight();
        float screenAspect = screenWidth / screenHeight;
        
        float quadWidth, quadHeight;
        if (videoAspect > screenAspect) {
            quadHeight = screenHeight;
            quadWidth = screenHeight * videoAspect;
        } else {
            quadWidth = screenWidth;
            quadHeight = screenWidth / videoAspect;
        }
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        
        float x0 = -quadWidth / 2f;
        float y0 = -quadHeight / 2f;
        float x1 = quadWidth / 2f;
        float y1 = quadHeight / 2f;
        
        // Fixed texture coordinates - inverted horizontally to fix rotation
        buf.vertex(matrix, x0, y1, 0).texture(1, 0);  // Top-left vertex, top-right texture
        buf.vertex(matrix, x1, y1, 0).texture(0, 0);  // Top-right vertex, top-left texture
        buf.vertex(matrix, x1, y0, 0).texture(0, 1);  // Bottom-right vertex, bottom-left texture
        buf.vertex(matrix, x0, y0, 0).texture(1, 1);  // Bottom-left vertex, bottom-right texture
        
        BufferRenderer.drawWithGlobalProgram(buf.end());
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}


