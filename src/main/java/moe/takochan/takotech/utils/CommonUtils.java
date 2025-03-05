package moe.takochan.takotech.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.FMLCommonHandler;
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

    public static NBTTagCompound openNbtData(final ItemStack i) {
        NBTTagCompound compound = i.getTagCompound();

        if (compound == null) {
            i.setTagCompound(compound = new NBTTagCompound());
        }

        return compound;
    }

    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }
}
