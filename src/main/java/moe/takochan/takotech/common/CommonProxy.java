package moe.takochan.takotech.common;

import net.minecraftforge.common.MinecraftForge;

import appeng.api.AEApi;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.event.PlayerDestroyItemEventHandler;
import moe.takochan.takotech.common.event.RenderGameOverlayEventHandler;
import moe.takochan.takotech.common.event.WorldEventHandler;
import moe.takochan.takotech.common.loader.BlockLoader;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.common.loader.ModLoader;
import moe.takochan.takotech.common.loader.RecipeLoader;
import moe.takochan.takotech.common.storage.CellItemSavedData;
import moe.takochan.takotech.common.storage.TakoCellHandler;
import moe.takochan.takotech.config.TakoTechConfig;
import moe.takochan.takotech.crossmod.Waila;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
        MinecraftForge.EVENT_BUS.register(new RenderGameOverlayEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerDestroyItemEventHandler());
        // 配置初始化
        TakoTechConfig.init();
        // ModLoader
        new ModLoader().run();
        // 方块初始化
        new BlockLoader().run();
        // 物品初始化
        new ItemLoader().run();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        if (ModLoader.WAILA) {
            Waila.run();
        }
        // 注册配方
        new RecipeLoader().run();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {

        GuiType.register();
        // 注册CellHandler
        AEApi.instance()
            .registries()
            .cell()
            .addCellHandler(new TakoCellHandler());
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
