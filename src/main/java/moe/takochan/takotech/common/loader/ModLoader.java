package moe.takochan.takotech.common.loader;

import cpw.mods.fml.common.Loader;

public class ModLoader implements Runnable {

    public static boolean WAILA = false;

    @Override
    public void run() {
        if (Loader.isModLoaded("Waila")) {
            WAILA = true;
        }
    }
}
