package moe.takochan.takotech.common;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import moe.takochan.takotech.common.event.WorldEventHandler;
import moe.takochan.takotech.common.loader.RecipeLoader;
import moe.takochan.takotech.common.storage.CellItemSavedData;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        RecipeLoader.init();
        RecipeLoader.registryRecipe();
    }

    public void complete(FMLLoadCompleteEvent event) {

    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}

    public void serverStopping(FMLServerStartingEvent event) {
        CellItemSavedData cellData = CellItemSavedData.getInstance();
        if (cellData != null) {
            cellData.setDirty(true);
        }
    }
}
