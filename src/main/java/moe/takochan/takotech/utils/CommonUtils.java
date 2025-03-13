package moe.takochan.takotech.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.FMLCommonHandler;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.Reference;

public class CommonUtils {

    /**
     * 判断当前是否为服务器端
     *
     * @return 如果是服务器端，返回 true；否则返回 false
     */
    public static boolean isServer() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer();
    }

    /**
     * 判断当前是否为客户端
     *
     * @return 如果是客户端，返回 true；否则返回 false
     */
    public static boolean isClient() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient();
    }

    /**
     * 打开或创建物品的 NBT 数据
     *
     * @param itemStack 物品堆栈
     * @return 物品的 NBT 数据
     */
    public static NBTTagCompound openNbtData(final ItemStack itemStack) {
        NBTTagCompound compound = itemStack.getTagCompound();

        if (compound == null) {
            itemStack.setTagCompound(compound = new NBTTagCompound());
        }

        return compound;
    }

    /**
     * 生成资源路径
     *
     * @param name 资源名称
     * @return 资源路径
     */
    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }

    /**
     * 打开 GUI
     *
     * @param type   GUI 类型
     * @param player 玩家
     * @param tile   关联的 TileEntity
     */
    public static void openGui(final GuiType type, final EntityPlayer player, final TileEntity tile) {
        // 如果是客户端，直接返回
        if (isClient()) return;

        // 获取坐标
        int x = tile != null ? tile.xCoord : (int) player.posX;
        int y = tile != null ? tile.yCoord : (int) player.posY;
        int z = tile != null ? tile.zCoord : (int) player.posZ;

        // 根据 TileEntity 是否存在，打开对应的 GUI
        if (tile == null && type.tileClass == null) {
            player.openGui(TakoTechMod.instance, type.ordinal(), player.worldObj, x, y, z);
        } else if (tile != null && type.tileClass != null) {
            player.openGui(TakoTechMod.instance, type.ordinal(), tile.getWorldObj(), x, y, z);
        }
    }

}
