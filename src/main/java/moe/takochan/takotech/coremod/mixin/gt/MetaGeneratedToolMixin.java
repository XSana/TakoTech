package moe.takochan.takotech.coremod.mixin.gt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

@Mixin(value = MetaGeneratedTool.class, remap = false)
public abstract class MetaGeneratedToolMixin {

    /**
     * 覆写获取容器物品逻辑 当耐久耗尽时返回配置的工具箱物品
     *
     * @author XSana
     * @reason 实现工具箱物品自动回收功能
     */
    @Inject(method = "getContainerItem", at = @At("RETURN"), cancellable = true)
    public void getContainerItem(ItemStack aStack, CallbackInfoReturnable<ItemStack> cir) {
        final ItemStack vanillaResult = cir.getReturnValue();

        if (vanillaResult == null || vanillaResult.stackSize <= 0) {
            final NBTTagCompound rootTag = CommonUtils.openNbtData(aStack);

            if (rootTag.hasKey(NBTConstants.TOOLBOX_DATA)) {
                final NBTTagCompound toolboxItems = rootTag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                cir.setReturnValue(ItemStack.loadItemStackFromNBT(toolboxItems));
            }
        }
    }

}
