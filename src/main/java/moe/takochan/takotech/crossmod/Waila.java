package moe.takochan.takotech.crossmod;

import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import moe.takochan.takotech.common.tile.BaseAETile;
import moe.takochan.takotech.crossmod.waila.TileWailaDataProvider;

public class Waila {

    public static void run() {
        FMLInterModComms.sendMessage("Waila", "register", Waila.class.getName() + ".register");
    }

    public static void register(final IWailaRegistrar registrar) {
        final IWailaDataProvider tile = new TileWailaDataProvider();

        registrar.registerBodyProvider(tile, BaseAETile.class);
        registrar.registerNBTProvider(tile, BaseAETile.class);
    }

}
