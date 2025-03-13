package moe.takochan.takotech.coremod.mixin.ic2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.items.MetaGeneratedTool;
import ic2.api.item.ItemWrapper;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

@Mixin(value = ItemWrapper.class, remap = false)
public class ItemWrapperMixin {

    /**
     * 注入方法：在 ItemWrapper 的 canBeStoredInToolbox 方法返回时执行
     * <p>
     * 用于检查物品是否可以存储在工具箱中，如果物品是 MetaGeneratedTool 且包含工具箱数据，则返回 false
     *
     * @param stack 物品堆栈
     * @param cir   回调信息，用于设置返回值
     */
    @Inject(method = "canBeStoredInToolbox", at = @At("RETURN"), cancellable = true)
    private static void canBeStoredInToolbox(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null) {
            // 检查物品是否为 MetaGeneratedTool
            if (stack.getItem() instanceof MetaGeneratedTool) {
                NBTTagCompound nbt = CommonUtils.openNbtData(stack);
                if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) cir.setReturnValue(false);
            }
        }
    }
}
