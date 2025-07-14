package moe.takochan.takotech.client.render;

import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.ae.ItemOreStorageCell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class OreStorageCellRenderer implements IItemRenderer {


    public OreStorageCellRenderer() {
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY
            && item != null
            && item.getItem() instanceof ItemOreStorageCell
            && item.getItemDamage() > 0;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemOreStorageCell cell)) return;
        int meta = item.getItemDamage();

        IIcon overlay = cell.getOverlayIcon(meta);
        IIcon baseIcon = item.getIconIndex();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        // GL11.glScalef(1F, 1F, 1F);
        // GL11.glTranslatef(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(0F, 1F, 0F, 0F);

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationItemsTexture);

        if (baseIcon != null) {
            drawIcon(baseIcon);
        }

        if (overlay != null) {
            float scale = 0.5f;
            float offset = (1.0f - scale) * 16.0f;
            GL11.glPushMatrix();
            GL11.glTranslatef(offset, offset, 0.01f);
            GL11.glScalef(scale, scale, 1.0f);

            drawIcon(overlay);

            GL11.glPopMatrix();
        }

        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawIcon(IIcon icon) {
        Tessellator t = Tessellator.instance;

        float minU = icon.getMinU();
        float maxU = icon.getMaxU();
        float minV = icon.getMinV();
        float maxV = icon.getMaxV();

        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(0, 1, 0, minU, maxV);
        t.addVertexWithUV(1, 1, 0, maxU, maxV);
        t.addVertexWithUV(1, 0, 0, maxU, minV);
        t.addVertexWithUV(0, 0, 0, minU, minV);
        t.draw();
    }
}
