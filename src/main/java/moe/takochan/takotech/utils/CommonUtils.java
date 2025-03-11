package moe.takochan.takotech.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.FMLCommonHandler;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.Reference;

public class CommonUtils {

    public static boolean isServer() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer();
    }

    public static boolean isClient() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient();
    }

    public static NBTTagCompound openNbtData(final ItemStack itemStack) {
        NBTTagCompound compound = itemStack.getTagCompound();

        if (compound == null) {
            itemStack.setTagCompound(compound = new NBTTagCompound());
        }

        return compound;
    }

    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }

    public static void openGui(final GuiType type, final EntityPlayer player, final TileEntity tile) {
        if (isClient()) {
            return;
        }

        int x = tile != null ? tile.xCoord : (int) player.posX;
        int y = tile != null ? tile.yCoord : (int) player.posY;
        int z = tile != null ? tile.zCoord : (int) player.posZ;

        if (tile == null && type.tileClass == null) {
            player.openGui(TakoTechMod.instance, type.ordinal(), player.worldObj, x, y, z);
        } else if (tile != null && type.tileClass != null) {
            player.openGui(TakoTechMod.instance, type.ordinal(), tile.getWorldObj(), x, y, z);
        }
    }

    public static byte[] nbtToBytes(NBTTagCompound tag) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            TakoTechMod.LOG.error("Failed to convert NBTTagCompound to byte array!", e);
        }
        return new byte[0];
    }

    public static NBTTagCompound bytesToNbt(byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return CompressedStreamTools.readCompressed(inputStream);
        } catch (IOException e) {
            TakoTechMod.LOG.error("Failed to convert byte array to NBTTagCompound!", e);
        }
        return new NBTTagCompound();
    }
}
