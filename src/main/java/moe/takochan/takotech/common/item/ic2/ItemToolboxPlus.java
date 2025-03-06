package moe.takochan.takotech.common.item.ic2;

import static moe.takochan.takotech.client.gui.settings.GameSettings.selectTool;

import java.util.ArrayList;
import java.util.List;

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
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.tool.HandHeldToolbox;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemToolboxPlus extends BaseItemToolbox implements IHandHeldInventory {

    @SideOnly(Side.CLIENT)
    private IIcon baseIcon;

    public ItemToolboxPlus() {
        super(NameConstants.ITEM_TOOLBOX_PLUS);
        this.setMaxStackSize(1);
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_TOOLBOX_PLUS));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack itemStack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        lines.add(
            I18nUtils.tooltip(
                NameConstants.ITEM_TOOLBOX_PLUS_DESC,
                GameSettings.getKeyDisplayString(selectTool.getKeyCode())));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        this.baseIcon = register.registerIcon(this.getIconString());
    }

    @Override
    public void register() {
        GameRegistry.registerItem(this, NameConstants.ITEM_TOOLBOX_PLUS);
        setCreativeTab(TakoTechTabs.getInstance());
    }


    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }

    public static List<ItemStack> getToolItems(ItemStack itemStack) {
        List<ItemStack> list = new ArrayList<>();
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        if (nbt.hasKey(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_LIST)) {
            NBTTagList itemsTagList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < itemsTagList.tagCount(); i++) {
                NBTTagCompound itemTag = itemsTagList.getCompoundTagAt(i);
                if (itemTag.hasKey(NBTConstants.TOOLBOX_SELECTED)) continue;
                ItemStack toolStack = ItemStack.loadItemStackFromNBT(itemTag);
                if (toolStack != null && toolStack.getItem() instanceof MetaGeneratedTool) {
                    list.add(toolStack);
                }
            }
        }
        return list;
    }

    public static ItemStack getSelectedItemStack(ItemStack itemStack, int selectedIndex) {
        if (selectedIndex >= 0) {
            List<ItemStack> tools = getToolItems(itemStack);
            if (selectedIndex < tools.size()) {
                return tools.get(selectedIndex);
            }
        }
        return null;
    }

    public static void processSelection(EntityPlayer player, ItemStack selectedStack) {
        if (CommonUtils.isClient() || selectedStack == null) return;

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null) return;

        ItemStack toolbox = null;

        if (heldItem.getItem() instanceof ItemToolboxPlus) {
            toolbox = heldItem;
        } else if (heldItem.getItem() instanceof MetaGeneratedTool) {
            NBTTagCompound nbt = CommonUtils.openNbtData(heldItem);
            if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) {
                NBTTagList itemsTagList = (NBTTagList) nbt
                    .getTagList(NBTConstants.TOOLBOX_DATA, Constants.NBT.TAG_COMPOUND)
                    .copy();
                int selectIndex = nbt.getInteger(NBTConstants.TOOLBOX_SELECTED_INDEX);

                NBTTagCompound selectedItem = itemsTagList.getCompoundTagAt(selectIndex);
                if (selectedItem != null && selectedItem.hasKey(NBTConstants.TOOLBOX_SELECTED)) {
                    selectedItem.removeTag(NBTConstants.TOOLBOX_SELECTED);
                    nbt.removeTag(NBTConstants.TOOLBOX_DATA);
                    nbt.removeTag(NBTConstants.TOOLBOX_SELECTED_INDEX);
                    heldItem.writeToNBT(selectedItem);
                } else {
                    return;
                }
                toolbox = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);
                CommonUtils.openNbtData(toolbox)
                    .setTag(NBTConstants.TOOLBOX_ITEMS, itemsTagList);
            }
        } else {
            return;
        }

        List<ItemStack> toolItems = getToolItems(toolbox);

        int index = -1;
        if (!(selectedStack.getItem() instanceof ItemToolboxPlus)) {
            for (int i = 0; i < toolItems.size(); i++) {
                if (toolItems.get(i)
                    .isItemEqual(selectedStack)) {
                    index = i;
                    break;
                }
            }
        }
        // 执行后续物品切换逻辑
        updateToolboxContents(toolbox, player, index);
    }

    private static void updateToolboxContents(ItemStack itemStack, EntityPlayer player, int index) {
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        // 根据索引更新手持物品逻辑
        if (index == -1) {
            // nbt.removeTag(NBTConstants.TOOLBOX_SELECTED_INDEX);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, itemStack);
        } else {
            // 获取容器内物品列表
            ItemStack selectedTool = getSelectedItemStack(itemStack, index);
            if (selectedTool == null) {
                return;
            }
            // 复制物品，防止修改原物品
            selectedTool = selectedTool.copy();

            // 获取物品列表NBT
            NBTTagList itemsTagList = nbt.getTagList(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND);
            // 构建占位NBT
            NBTTagCompound tempNbt = new NBTTagCompound();
            tempNbt.setBoolean(NBTConstants.TOOLBOX_SELECTED, true);
            byte slot = itemsTagList.getCompoundTagAt(index)
                .getByte(NBTConstants.TOOLBOX_TOOLS_SLOT);
            tempNbt.setByte(NBTConstants.TOOLBOX_TOOLS_SLOT, slot);
            // 将占位NBT替换到指定位置
            itemsTagList.func_150304_a(index, tempNbt);

            // 将工具箱数据写入工具
            NBTTagCompound selectedToolNbt = CommonUtils.openNbtData(selectedTool);
            selectedToolNbt.setTag(NBTConstants.TOOLBOX_DATA, itemsTagList);
            selectedToolNbt.setInteger(NBTConstants.TOOLBOX_SELECTED_INDEX, index);

            player.inventory.setInventorySlotContents(player.inventory.currentItem, selectedTool);
        }

        player.inventory.markDirty();

        // 同步容器数据给客户端
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

}
