package com.xdw.spiceoflifelatiao.linkage.jade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

public class CustomSpriteElement extends Element {
    private final ResourceLocation sprite;
    private final int textureWidth;
    private final int textureHeight;
    private final int uPosition;
    private final int vPosition;
    private final int uWidth;
    private final int vHeight;

    public CustomSpriteElement(ResourceLocation sprite,
                               int uPosition,
                               int vPosition,
                               int uWidth,
                               int vHeight,
                               int textureWidth,
                               int textureHeight) {
        this.sprite = sprite;
        this.uPosition = uPosition;
        this.vPosition = vPosition;
        this.uWidth = uWidth;
        this.vHeight = vHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public Vec2 getSize() {
        return new Vec2((float)uWidth, (float)vHeight);
    }

    public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(sprite, (int) x, (int) y, (float) uPosition, (float) vPosition, uWidth, vHeight, textureWidth, textureHeight);
    }
}
