package moe.takochan.takotech.mixin.gt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.common.render.GTRenderUtil;
import gregtech.common.render.MetaGeneratedToolRenderer;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

@Mixin(MetaGeneratedToolRenderer.class)
public abstract class MetaGeneratedToolRendererMixin {

    @Inject(method = "renderItem", at = @At("RETURN"), remap = false)
    private void afterRenderItem(IItemRenderer.ItemRenderType type, ItemStack stack, Object[] data, CallbackInfo ci) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            NBTTagCompound nbt = CommonUtils.openNbtData(stack);
            if (!nbt.hasKey(NBTConstants.TOOLBOX_DATA) || !nbt.getBoolean(NBTConstants.TOOLBOX_SELECTED_INDEX)) return;
            IIcon baseIcon = ItemLoader.ITEM_TOOLBOX_PLUS.getBaseIcon();
            if (baseIcon != null) {
                // 保存当前渲染状态
                GL11.glPushMatrix();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);

                double scale = 0.6;
                double offset = (1 - scale) * 16;
                GL11.glTranslated(offset, offset, 0.002);
                GL11.glScaled(scale, scale, 1.0);

                GTRenderUtil.renderItem(IItemRenderer.ItemRenderType.INVENTORY, baseIcon);

                // 恢复状态
                GL11.glPopMatrix();

            }
        }
    }
}
