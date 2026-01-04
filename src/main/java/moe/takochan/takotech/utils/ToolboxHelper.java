package moe.takochan.takotech.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;

/**
 * 工具箱相关的工具方法
 */
public class ToolboxHelper {

    private ToolboxHelper() {}

    /**
     * 获取工具箱中的工具列表
     *
     * @param itemStack 工具箱物品堆栈
     * @return 工具列表
     */
    public static List<ToolData> getToolItems(ItemStack itemStack) {
        List<ToolData> list = new ArrayList<>();
        if (!isItemToolbox(itemStack)) return list;

        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        if (nbt.hasKey(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_LIST)) {
            NBTTagList contentList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < contentList.tagCount(); i++) {
                NBTTagCompound slotNbt = contentList.getCompoundTagAt(i);
                int slot = slotNbt.getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
                ItemStack toolStack = ItemStack.loadItemStackFromNBT(slotNbt);
                if (toolStack != null && toolStack.getItem() instanceof MetaGeneratedTool) {
                    list.add(new ToolData(slot, toolStack));
                }
            }
        }
        return list;
    }

    /**
     * 将工具设置到指定槽位
     *
     * @param itemStack 工具箱物品堆栈
     * @param slot      槽位
     * @param toolStack 工具物品堆栈
     */
    public static void setItemToSlot(ItemStack itemStack, int slot, ItemStack toolStack) {
        if (!isItemToolbox(itemStack)) return;

        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        NBTTagList contentList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
        ItemStack[] toolStackList = new ItemStack[9];
        for (int i = 0; i < contentList.tagCount(); i++) {
            NBTTagCompound slotNbt = contentList.getCompoundTagAt(i);
            int slotNum = slotNbt.getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
            toolStackList[slotNum] = ItemStack.loadItemStackFromNBT(slotNbt);
        }

        toolStackList[slot] = toolStack;

        NBTTagList newContentList = new NBTTagList();
        for (int i = 0; i < toolStackList.length; i++) {
            if (toolStackList[i] != null) {
                NBTTagCompound slotNbt = new NBTTagCompound();
                slotNbt.setByte(NBTConstants.TOOLBOX_TOOLS_SLOT, (byte) i);
                toolStackList[i].writeToNBT(slotNbt);
                newContentList.appendTag(slotNbt);
            }
        }
        nbt.setTag(NBTConstants.TOOLBOX_ITEMS, newContentList);
    }

    /**
     * 获取指定槽位的工具堆栈
     *
     * @param itemStack 工具箱物品堆栈
     * @param slot      槽位
     * @return 工具堆栈
     */
    public static ItemStack getStackInSlot(ItemStack itemStack, int slot) {
        if (!isItemToolbox(itemStack)) return null;

        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        if (nbt.hasKey(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_LIST)) {
            NBTTagList contentList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < contentList.tagCount(); i++) {
                NBTTagCompound slotNbt = contentList.getCompoundTagAt(i);
                int slotNum = slotNbt.getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
                if (slotNum == slot) {
                    return ItemStack.loadItemStackFromNBT(slotNbt);
                }
            }
        }
        return null;
    }

    /**
     * 获取工具箱物品堆栈
     *
     * @param itemStack 物品堆栈
     * @return 工具箱物品堆栈（如果存在）
     */
    public static Optional<ItemStack> getToolbox(ItemStack itemStack) {
        if (!isItemToolbox(itemStack)) {
            return Optional.empty();
        }
        if (isToolboxPlus(itemStack)) {
            return Optional.of(itemStack);
        }
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) {
            return Optional.ofNullable(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(NBTConstants.TOOLBOX_DATA)));
        }
        return Optional.empty();
    }

    /**
     * 检查物品是否为工具箱或包含工具箱数据
     *
     * @param itemStack 物品堆栈
     * @return 如果是工具箱，返回true；否则返回false
     */
    public static boolean isItemToolbox(ItemStack itemStack) {
        return isToolboxPlus(itemStack) || (isMetaGeneratedTool(itemStack) && CommonUtils.openNbtData(itemStack)
            .hasKey(NBTConstants.TOOLBOX_DATA));
    }

    /**
     * 检查物品是否为高级工具箱
     *
     * @param itemStack 物品堆栈
     * @return 如果是高级工具箱，返回true；否则返回false
     */
    public static boolean isToolboxPlus(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() instanceof ItemToolboxPlus;
    }

    /**
     * 检查物品是否为MetaGeneratedTool
     *
     * @param itemStack 物品堆栈
     * @return 如果是MetaGeneratedTool，返回true；否则返回false
     */
    public static boolean isMetaGeneratedTool(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() instanceof MetaGeneratedTool;
    }
}
