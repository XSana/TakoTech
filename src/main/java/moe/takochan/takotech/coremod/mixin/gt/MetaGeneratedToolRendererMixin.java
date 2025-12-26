package moe.takochan.takotech.coremod.mixin.gt;

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

    /**
     * 注入方法：在 MetaGeneratedToolRenderer 的 renderItem 方法返回时执行
     * <p>
     * 用于在物品渲染时添加工具箱的图标
     *
     * @param type  渲染类型
     * @param stack 物品堆栈
     * @param data  渲染数据
     * @param ci    回调信息
     */
    @Inject(method = "renderItem", at = @At("RETURN"), remap = false)
    private void afterRenderItem(IItemRenderer.ItemRenderType type, ItemStack stack, Object[] data, CallbackInfo ci) {
        // 检查渲染类型是否为库存渲染
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            // 获取物品的 NBT 数据
            NBTTagCompound nbt = CommonUtils.openNbtData(stack);
            // 检查是否存在工具箱数据和槽位数据
            if (!nbt.hasKey(NBTConstants.TOOLBOX_DATA) || !nbt.hasKey(NBTConstants.TOOLBOX_SLOT)) return;

            // 获取工具箱的基础图标
            IIcon baseIcon = ItemLoader.ITEM_TOOLBOX_PLUS.getBaseIcon();
            if (baseIcon != null) {
                // 保存矩阵和 GL 状态
                GL11.glPushMatrix();
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

                // 设置渲染状态
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_DEPTH_TEST);

                // 绑定物品纹理
                Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);

                // 设置缩放和偏移
                double scale = 0.6;
                double offset = (1 - scale) * 16;
                GL11.glTranslated(offset, offset, 0.002);
                GL11.glScaled(scale, scale, 1.0);

                // 渲染工具箱的图标
                GTRenderUtil.renderItem(IItemRenderer.ItemRenderType.INVENTORY, baseIcon);

                // 恢复 GL 状态和矩阵
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }
}
