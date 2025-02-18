package moe.takochan.takotech.common;

import net.minecraftforge.common.MinecraftForge;

import appeng.api.AEApi;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import moe.takochan.takotech.common.event.WorldEventHandler;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.common.loader.RecipeLoader;
import moe.takochan.takotech.common.storage.CellItemSavedData;
import moe.takochan.takotech.common.storage.TakoCellHandler;
import moe.takochan.takotech.config.TakoTechConfig;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
        TakoTechConfig.init();
        ItemLoader.registerItems();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        // 注册Recipe
        RecipeLoader.init();
        RecipeLoader.registryRecipe();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        // 注册CellHandler
        AEApi.instance()
            .registries()
            .cell()
            .addCellHandler(new TakoCellHandler());
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
