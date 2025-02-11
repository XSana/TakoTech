package moe.takochan.takotech.utils;

import moe.takochan.takotech.common.Reference;
import net.minecraft.util.ResourceLocation;

public class CommonUtils {

    public static String resource(String name) {
        return new ResourceLocation(Reference.MODID, name).toString();
    }
}
