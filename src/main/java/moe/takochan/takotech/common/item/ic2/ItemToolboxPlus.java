package moe.takochan.takotech.common.item.ic2;

import static moe.takochan.takotech.client.settings.GameSettings.selectTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.MetaGeneratedTool;
import ic2.core.item.IHandHeldInventory;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemToolboxPlus extends BaseItemToolbox implements IHandHeldInventory {

    // 基础图标
    @SideOnly(Side.CLIENT)
    private IIcon baseIcon;

    public ItemToolboxPlus() {
        super(NameConstants.ITEM_TOOLBOX_PLUS);
        // 设置最大堆叠数为1
        this.setMaxStackSize(1);
        // 设置材质路径
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_TOOLBOX_PLUS));
    }

    /**
     * 获取工具箱中的工具列表
     *
     * @param itemStack 工具箱物品堆栈
     * @return 工具列表
     */
    public static List<ToolData> getToolItems(ItemStack itemStack) {
        // 可用工具列表
        List<ToolData> list = new ArrayList<>();
        if (!isItemToolbox(itemStack)) return list;

        // 获取NBT
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        // 获取物品列表
        if (nbt.hasKey(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_LIST)) {
            NBTTagList contentList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < contentList.tagCount(); i++) {
                NBTTagCompound slotNbt = contentList.getCompoundTagAt(i);
                // 获取槽位
                int slot = slotNbt.getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
                // 加载工具物品堆栈
                ItemStack toolStack = ItemStack.loadItemStackFromNBT(slotNbt);
                if (toolStack != null && toolStack.getItem() instanceof MetaGeneratedTool) {
                    // 添加到工具列表
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
        // 如果不是工具箱，直接返回
        if (!isItemToolbox(itemStack)) return;

        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        NBTTagList contentList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
        // 创建工具堆栈数组
        ItemStack[] toolStackList = new ItemStack[9];
        for (int i = 0; i < contentList.tagCount(); i++) {
            NBTTagCompound slotNbt = contentList.getCompoundTagAt(i);
            // 获取槽位
            int slotNum = slotNbt.getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
            // 加载工具堆栈
            toolStackList[slotNum] = ItemStack.loadItemStackFromNBT(slotNbt);
        }

        // 设置指定槽位的工具
        toolStackList[slot] = toolStack;

        // 创建新的NBT列表
        NBTTagList newContentList = new NBTTagList();
        for (int i = 0; i < toolStackList.length; i++) {
            if (toolStackList[i] != null) {
                NBTTagCompound slotNbt = new NBTTagCompound();
                // 设置槽位
                slotNbt.setByte(NBTConstants.TOOLBOX_TOOLS_SLOT, (byte) i);
                // 写入工具堆栈
                toolStackList[i].writeToNBT(slotNbt);
                // 添加到新列表
                newContentList.appendTag(slotNbt);
            }
        }
        // 更新NBT数据
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
        // 如果不是工具箱，返回null
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
            // 如果不是工具箱，返回空
            return Optional.empty();
        }
        if (isToolboxPlus(itemStack)) {
            // 如果是高级工具箱，返回当前堆栈
            return Optional.of(itemStack);
        }
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) {
            // 返回工具箱数据
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
     * 检查物品是否为工具箱
     *
     * @param itemStack 物品堆栈
     * @return 如果是工具箱，返回true；否则返回false
     */
    public static boolean isToolboxPlus(ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemToolboxPlus;
    }

    /**
     * 检查物品是否为MetaGeneratedTool
     *
     * @param itemStack 物品堆栈
     * @return 如果是MetaGeneratedTool，返回true；否则返回false
     */
    public static boolean isMetaGeneratedTool(ItemStack itemStack) {
        return itemStack.getItem() instanceof MetaGeneratedTool;
    }

    /**
     * 添加物品信息
     *
     * @param itemStack       物品堆栈
     * @param player          玩家
     * @param lines           信息列表
     * @param displayMoreInfo 是否显示更多信息
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack itemStack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        lines.add(
            I18nUtils.tooltip(
                NameConstants.ITEM_TOOLBOX_PLUS_DESC,
                GameSettings.getKeyDisplayString(selectTool.getKeyCode())));
    }

    /**
     * 注册图标
     *
     * @param register 图标注册器
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        this.baseIcon = register.registerIcon(this.getIconString());
    }

    /**
     * 注册物品
     */
    @Override
    public void register() {
        GameRegistry.registerItem(this, NameConstants.ITEM_TOOLBOX_PLUS);
        setCreativeTab(TakoTechTabs.getInstance());
    }

    /**
     * 获取基础图标
     *
     * @return 基础图标
     */
    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }

}
