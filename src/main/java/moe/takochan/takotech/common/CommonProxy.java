package moe.takochan.takotech.common;

import appeng.util.Platform;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.storage.StorageCellSaveData;
import moe.takochan.takotech.config.TakoTechConfig;
import net.minecraftforge.event.world.WorldEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        TakoTechConfig.init();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
    }

    public void serverStopping(FMLServerStartingEvent event) {
        StorageCellSaveData cellData = StorageCellSaveData.getInstance();
        if (cellData != null) {
            cellData.setDirty(true);
        }
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (Platform.isServer() && event.world.provider.dimensionId == 0) {
            StorageCellSaveData.init(event.world);
            TakoTechMod.LOG.info("StorageCellData initialized successfully!");
        }
    }
}
