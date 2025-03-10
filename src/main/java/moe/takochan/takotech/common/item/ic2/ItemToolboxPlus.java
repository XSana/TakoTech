package moe.takochan.takotech.common.item.ic2;

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
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

import static moe.takochan.takotech.client.gui.settings.GameSettings.selectTool;

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

    /**
     * 获取可用GT工具
     *
     * @param itemStack 当前物品堆
     * @param player    玩家实体
     * @return 可用物品清单及插槽下标
     */
    public List<ToolData> getGTTools(ItemStack itemStack, EntityPlayer player) {
        final List<ToolData> list = new ArrayList<>();
        if (CommonUtils.isClient()) return list;
        final IHasGui toolboxGUI = getInventory(player, itemStack);
        for (int i = 0; i < toolboxGUI.getSizeInventory(); i++) {
            final ItemStack tool = toolboxGUI.getStackInSlot(i);
            if (tool != null) {
                if (tool.stackSize <= 0) {
                    toolboxGUI.setInventorySlotContents(i, null);
                }
                if (tool.getItem() instanceof MetaGeneratedTool) {
                    list.add(new ToolData(i, tool));
                }
            }
        }
        return list;
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


}
