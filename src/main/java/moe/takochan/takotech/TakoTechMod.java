package moe.takochan.takotech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import appeng.api.AEApi;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import moe.takochan.takotech.common.CommonProxy;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.common.storage.TakoCellHandler;
import moe.takochan.takotech.config.TakoTechConfig;

@Mod(
    modid = Reference.MODID,
    name = Reference.MODNAME,
    version = Reference.VERSION,
    dependencies = Reference.DEPENDENCIES,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    guiFactory = Reference.GUI_FACTORY)
public class TakoTechMod {

    public static final Logger LOG = LogManager.getLogger(Reference.MODID);

    @Mod.Instance(Reference.MODID)
    public static TakoTechMod instance;

    @SidedProxy(
        clientSide = "moe.takochan.takotech.client.ClientProxy",
        serverSide = "moe.takochan.takotech.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        TakoTechConfig.init();
        proxy.preInit(event);
        ItemLoader.registerItems();
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        // 注册CellHandler
        AEApi.instance()
            .registries()
            .cell()
            .addCellHandler(new TakoCellHandler());
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void completeInit(FMLLoadCompleteEvent event) {
        proxy.complete(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStopping(FMLServerStartingEvent event) {
        proxy.serverStopping(event);
    }
}
