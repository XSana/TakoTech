package moe.takochan.takotech.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.IToolStats;
import gregtech.common.render.GTRenderUtil;
import gregtech.common.render.MetaGeneratedToolRenderer;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.ItemToolboxPlus;
import org.lwjgl.opengl.GL11;

public class ToolboxPlusRenderer extends MetaGeneratedToolRenderer {

    public ToolboxPlusRenderer() {
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return super.handleRenderType(stack, type);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return super.shouldUseRenderHelper(type, stack, helper);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        if (stack != null && stack.getItem() instanceof ItemToolboxPlus itme) {
            int index = itme.getSelectedIndex(stack);
            if (index >= 0) {
                ItemStack selected = itme.getSelectedItemStack(stack, index);
                super.renderItem(type, selected, data);
            } else {
                IIcon icon = itme.getBaseIcon();
                if (icon != null) {
                    Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(Reference.RESOURCE_ROOT_ID, icon.getIconName()));
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GTRenderUtil.renderItem(type, icon);
                }
            }
        }
    }

    private static void renderToolPart(ItemRenderType type, ItemStack stack, IToolStats toolStats, boolean isToolHead) {
        IIconContainer iconContainer = toolStats.getIcon(isToolHead, stack);
        if (iconContainer != null) {
            IIcon icon = iconContainer.getIcon();
            IIcon overlay = iconContainer.getOverlayIcon();
            if (icon != null) {
                Minecraft.getMinecraft().renderEngine.bindTexture(iconContainer.getTextureFile());
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                short[] modulation = toolStats.getRGBa(isToolHead, stack);
                GL11.glColor3f(modulation[0] / 255.0F, modulation[1] / 255.0F, modulation[2] / 255.0F);
                GTRenderUtil.renderItem(type, icon);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
            }
            if (overlay != null) {
                Minecraft.getMinecraft().renderEngine.bindTexture(iconContainer.getTextureFile());
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GTRenderUtil.renderItem(type, overlay);
            }
        }
    }
}
