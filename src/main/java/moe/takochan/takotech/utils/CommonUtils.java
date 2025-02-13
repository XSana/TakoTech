package moe.takochan.takotech.utils;

import net.minecraft.util.ResourceLocation;

import moe.takochan.takotech.common.Reference;

public class CommonUtils {

    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }
}
