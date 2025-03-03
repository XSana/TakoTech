package moe.takochan.takotech.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.tool.HandHeldToolbox;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;

public class ItemToolboxPlus extends BaseItem implements IHandHeldInventory {

    public ItemToolboxPlus() {
        super(NameConstants.ITEM_TOOLBOX_PLUS);
        this.setMaxStackSize(1);
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_TOOLBOX_PLUS));
    }

    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer) {
        if (IC2.platform.isSimulating()) {
            IC2.platform.launchGui(entityPlayer, this.getInventory(entityPlayer, itemStack));
        }

        return itemStack;
    }

    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    public void register() {
        GameRegistry.registerItem(this, NameConstants.ITEM_TOOLBOX_PLUS);
        setCreativeTab(TakoTechTabs.getInstance());
    }

    public IHasGui getInventory(EntityPlayer entityPlayer, ItemStack itemStack) {
        return new HandHeldToolbox(entityPlayer, itemStack, 9);
    }

    public static void processSelection(EntityPlayer player, int index) {
        ItemStack stack = player.getHeldItem();
        if (stack.getItem() instanceof ItemToolboxPlus) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) nbt = new NBTTagCompound();

            nbt.setInteger("selected", index);
            stack.setTagCompound(nbt);

            // 执行后续物品切换逻辑
            updateToolboxContents(player, index);
        }
    }

    private static void updateToolboxContents(EntityPlayer player, int index) {
        // 根据索引更新手持物品逻辑
        TakoTechMod.LOG.info("selected toolbox: {}", index);
    }
}
