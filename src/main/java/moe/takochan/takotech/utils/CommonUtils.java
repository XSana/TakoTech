package moe.takochan.takotech.utils;

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

    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }
}
