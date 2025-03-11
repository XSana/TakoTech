package moe.takochan.takotech.common.item.ic2;

import static moe.takochan.takotech.client.gui.settings.GameSettings.selectTool;

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
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemToolboxPlus extends BaseItemToolbox implements IHandHeldInventory {

    public final static ItemStack DEFAULT_ITEM = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);

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

    public static List<ToolData> getToolItems(ItemStack itemStack) {
        // 可用工具列表
        List<ToolData> list = new ArrayList<>();
        // 获取NBT
        NBTTagCompound nbt = CommonUtils.openNbtData(itemStack);
        // 获取物品列表
        if (nbt.hasKey(NBTConstants.TOOLBOX_ITEMS, Constants.NBT.TAG_COMPOUND)) {
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

    public static boolean isItemToolbox(ItemStack itemStack) {
        return isToolboxPlus(itemStack) || (isMetaGeneratedTool(itemStack) && CommonUtils.openNbtData(itemStack)
            .hasKey(NBTConstants.TOOLBOX_DATA));
    }

    public static boolean isToolboxPlus(ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemToolboxPlus;
    }

    public static boolean isMetaGeneratedTool(ItemStack itemStack) {
        return itemStack.getItem() instanceof MetaGeneratedTool;
    }

}
