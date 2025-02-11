package moe.takochan.takotech.common.event;

import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.storage.StorageCellSaveData;
import net.minecraftforge.event.world.WorldEvent;

public class WorldEventHandler {

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (Platform.isServer() && event.world.provider.dimensionId == 0) {
            StorageCellSaveData.init(event.world);
            TakoTechMod.LOG.info("StorageCellData initialized successfully!");
        }
    }
}
