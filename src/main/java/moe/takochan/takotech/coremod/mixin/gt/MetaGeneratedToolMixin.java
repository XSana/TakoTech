package moe.takochan.takotech.coremod.mixin.gt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import net.minecraftforge.common.util.Constants;

import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MetaGeneratedTool.class, remap = false)
public abstract class MetaGeneratedToolMixin {

    @Invoker("getContainerItem")
    public abstract ItemStack invokerGetContainerItem(ItemStack aStack, boolean playSound);

    /**
     * 覆写容器物品存在判断逻辑
     * 当工具耐久耗尽且包含工具箱数据时返回true
     *
     * @author XSana
     * @reason 支持工具箱系统特殊逻辑
     */
    @Overwrite
    public final boolean hasContainerItem(ItemStack aStack) {
        final ItemStack simulatedResult = invokerGetContainerItem(aStack, false);

        // 当原版容器物品不存在时检查工具箱数据
        if ((simulatedResult == null || simulatedResult.stackSize <= 0)) {
            final NBTTagCompound rootTag = CommonUtils.openNbtData(aStack);
            return rootTag.hasKey(NBTConstants.TOOLBOX_DATA);
        }

        return true;
    }

    /**
     * 覆写获取容器物品逻辑
     * 当耐久耗尽时返回配置的工具箱物品
     *
     * @author XSana
     * @reason 实现工具箱物品自动回收功能
     */
    @Overwrite
    public final ItemStack getContainerItem(ItemStack aStack) {
        final ItemStack vanillaResult = invokerGetContainerItem(aStack, true);

        // 仅当原版容器物品无效时处理工具箱逻辑
        if (vanillaResult == null || vanillaResult.stackSize <= 0) {
            final NBTTagCompound rootTag = CommonUtils.openNbtData(aStack);

            if (rootTag.hasKey(NBTConstants.TOOLBOX_DATA)) {
                final NBTTagList toolboxItems = rootTag
                    .getTagList(NBTConstants.TOOLBOX_DATA, Constants.NBT.TAG_COMPOUND);

                // 创建新的工具箱物品
                final ItemStack toolbox = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);
                final NBTTagCompound newTag = CommonUtils.openNbtData(toolbox);

                // 清理选择状态后保存数据
                takotech$removeSelectionTags(toolboxItems);
                newTag.setTag(NBTConstants.TOOLBOX_ITEMS, toolboxItems);

                return toolbox;
            }
        }

        return vanillaResult;
    }

    /**
     * 移除工具箱物品的选择状态标记
     */
    @Unique
    private void takotech$removeSelectionTags(NBTTagList toolboxItems) {
        // 倒序遍历避免索引错位
        for (int i = toolboxItems.tagCount() - 1; i >= 0; i--) {
            final NBTTagCompound toolTag = toolboxItems.getCompoundTagAt(i);
            if (toolTag.hasKey(NBTConstants.TOOLBOX_SELECTED)) {
                toolTag.removeTag(NBTConstants.TOOLBOX_SELECTED);
            }
        }
    }
}
