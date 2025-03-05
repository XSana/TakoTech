package moe.takochan.takotech.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import gregtech.common.render.GTRenderUtil;
import gregtech.common.render.MetaGeneratedToolRenderer;
import moe.takochan.takotech.common.item.ItemToolboxPlus;

public class ToolboxPlusRenderer extends MetaGeneratedToolRenderer {

    public ToolboxPlusRenderer() {}

    @Override
    public boolean handleRenderType(ItemStack itemStack, ItemRenderType type) {
        return super.handleRenderType(itemStack, type);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack itemStack, ItemRendererHelper helper) {
        return super.shouldUseRenderHelper(type, itemStack, helper);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack itemStack, Object... data) {
        if (itemStack != null && itemStack.getItem() instanceof ItemToolboxPlus item) {
            int index = item.getSelectedIndex(itemStack);
            if (index >= 0) {
                ItemStack selected = item.getSelectedItemStack(itemStack, index);
                if (selected != null) {
                    super.renderItem(type, selected, data);
                }
                if (type == ItemRenderType.INVENTORY) {
                    IIcon baseIcon = item.getBaseIcon();
                    if (baseIcon != null) {
                        // 保存当前渲染状态
                        GL11.glPushMatrix();
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);

                        double scale = 0.6;
                        double offset = (1 - scale) * 16;
                        GL11.glTranslated(offset, offset, 0.002);
                        GL11.glScaled(scale, scale, 1.0);

                        GTRenderUtil.renderItem(ItemRenderType.INVENTORY, baseIcon);

                        // 恢复状态
                        GL11.glPopMatrix();

                    }
                }
            } else {
                GL11.glEnable(GL11.GL_BLEND);
                GTRenderUtil.applyStandardItemTransform(type);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);

                IIcon icon = item.getBaseIcon();
                if (icon != null) {
                    Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);

                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GTRenderUtil.renderItem(type, icon);
                }
            }
        }
    }
}
