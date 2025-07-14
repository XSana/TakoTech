package moe.takochan.takotech.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import moe.takochan.takotech.common.item.ae.ItemOreStorageCell;

public class OreStorageCellRenderer implements IItemRenderer {

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        // return false;
        return type == ItemRenderType.INVENTORY && item != null
            && item.getItem() instanceof ItemOreStorageCell
            && item.getItemDamage() > 0;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemOreStorageCell cell)) return;
        int meta = item.getItemDamage();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // 获取物品的基础图标
        final IIcon baseIcon = item.getIconIndex();
        final IIcon overlay = cell.getOverlayIcon(meta);

        // 基础设置
        GL11.glColor4f(1, 1, 1, 1.0F);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        // 物品栏变换
        GL11.glScalef(16F, 16F, 10F);
        GL11.glTranslatef(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);

        drawIcon(baseIcon);

        if (overlay != null) {
            GL11.glPushMatrix();

            // 将叠加图标缩小到原大小的1/2
            float scale = 0.4f;
            GL11.glScalef(scale, scale, scale);

            // 移动到右下角 (计算缩放后的位置)
            float offsetX = (1 - scale) / scale;
            float offsetY = 0.01f;
            GL11.glTranslatef(offsetX, offsetY, -0.1f);

            drawIcon(overlay);

            GL11.glPopMatrix();
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static void drawIcon(IIcon icon) {
        final Tessellator tessellator = Tessellator.instance;

        // 获取图标的UV坐标
        final float f4 = icon.getMinU();
        final float f5 = icon.getMaxU();
        final float f6 = icon.getMinV();
        final float f7 = icon.getMaxV();

        // 绘制图标
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV(0, 0, 0, f4, f7);
        tessellator.addVertexWithUV(1, 0, 0, f5, f7);
        tessellator.addVertexWithUV(1, 1, 0, f5, f6);
        tessellator.addVertexWithUV(0, 1, 0, f4, f6);
        tessellator.draw();
    }
}
