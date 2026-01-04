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
     * <p>
     * 注意：某些 mod 可能在非耐久耗尽时调用 getContainerItem，
     * 需要验证工具确实耗尽才返还工具箱。
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
                // 验证工具确实已耗尽（防止其他 mod 异常调用）
                long damage = MetaGeneratedTool.getToolDamage(aStack);
                long maxDamage = MetaGeneratedTool.getToolMaxDamage(aStack);
                if (damage < maxDamage) {
                    // 工具未真正耗尽，不返还工具箱
                    return;
                }

                final NBTTagCompound toolboxItems = rootTag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                cir.setReturnValue(ItemStack.loadItemStackFromNBT(toolboxItems));
            }
        }
    }

}
