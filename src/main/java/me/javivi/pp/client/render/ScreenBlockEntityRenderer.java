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

public final class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    public ScreenBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ScreenBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        be.clientEnsureStarted();
        int tex = be.clientPreRender();

        if (tex <= 0) return;

        Direction facing = be.getCachedState().get(ScreenBlock.FACING);

        be.ensureRegionComputed();
        boolean controller = be.isController();
        BlockPos min = be.regionMin();
        BlockPos max = be.regionMax();

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);

        float centerDx = 0f, centerDy = 0f, centerDz = 0f;
        if (controller) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                centerDx = ((min.getX() + max.getX()) / 2.0f) - be.getPos().getX();
                centerDy = ((min.getY() + max.getY()) / 2.0f) - be.getPos().getY();
            } else {
                centerDz = ((min.getZ() + max.getZ()) / 2.0f) - be.getPos().getZ();
                centerDy = ((min.getY() + max.getY()) / 2.0f) - be.getPos().getY();
            }
            matrices.translate(centerDx, centerDy, centerDz);
        }

        switch (facing) {
            case NORTH -> {}
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f));
            default -> {}
        }
        matrices.translate(0.0, 0.0, -0.501);


        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (!controller) {
            matrices.pop();
            return;
        }
        var pos = be.getPos();
        Identifier id = Identifier.of("pixelplay", "screen_runtime_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new me.javivi.pp.client.gui.ExternalTexture(tex));
        RenderSystem.setShaderTexture(0, id);

        float w = Math.max(1, be.videoWidth());
        float h = Math.max(1, be.videoHeight());
        try {
            var playerEnt = MinecraftClient.getInstance().player;
            if (playerEnt != null && be.clientPlayer() != null) {
                double cx, cy, cz;
                if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                    cx = (min.getX() + max.getX()) / 2.0 + 0.5;
                    cz = be.getPos().getZ() + 0.5;
                } else {
                    cx = be.getPos().getX() + 0.5;
                    cz = (min.getZ() + max.getZ()) / 2.0 + 0.5;
                }
                cy = (min.getY() + max.getY()) / 2.0 + 0.5;
                double dx = playerEnt.getX() - cx;
                double dy = playerEnt.getEyeY() - cy;
                double dz = playerEnt.getZ() - cz;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                double maxDist = 32.0; // distancia a 0 volumen
                float atten = (float)Math.max(0.0, 1.0 - dist / maxDist);
                float slider = me.javivi.pp.sound.MultimediaVolume.getMasterMultiplier();
                be.clientPlayer().setVolumeMultiplier(slider * atten);
            }
        } catch (Throwable ignored) {}
        float aspect = w / h;
        float blocksW = 1.0f;
        float blocksH = 1.0f;
        if (controller) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                blocksW = (max.getX() - min.getX() + 1);
                blocksH = (max.getY() - min.getY() + 1);
            } else {
                blocksW = (max.getZ() - min.getZ() + 1);
                blocksH = (max.getY() - min.getY() + 1);
            }
        }

        // COVER: llenar por completo la región recortando lo que sobre
        float regionAspect = blocksW / blocksH;
        float quadW, quadH;
        if (aspect > regionAspect) {
            // Vídeo más ancho: escalar por altura de la región y recortar a lo ancho
            quadH = blocksH;
            quadW = blocksH * aspect;
        } else if (aspect < regionAspect) {
            // Vídeo más alto: escalar por anchura de la región y recortar a lo alto
            quadW = blocksW;
            quadH = blocksW / aspect;
        } else {
            quadW = blocksW;
            quadH = blocksH;
        }

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float x0 = -quadW / 2f;
        float y0 = -quadH / 2f;
        float x1 = quadW / 2f;
        float y1 = quadH / 2f;
        buf.vertex(m, x0, y1, 0).texture(0, 0);
        buf.vertex(m, x1, y1, 0).texture(1, 0);
        buf.vertex(m, x1, y0, 0).texture(1, 1);
        buf.vertex(m, x0, y0, 0).texture(0, 1);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
        matrices.pop();
    }
}


