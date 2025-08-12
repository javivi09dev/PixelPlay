package me.javivi.pp.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;


public final class ExternalTexture extends AbstractTexture {
    private final int glId;

    public ExternalTexture(int glId) { this.glId = glId; }

    @Override
    public void load(ResourceManager manager) {}

    @Override
    public int getGlId() { return glId; }

    @Override
    public void bindTexture() { RenderSystem.bindTexture(glId); }

    @Override
    public void close() { }
}


