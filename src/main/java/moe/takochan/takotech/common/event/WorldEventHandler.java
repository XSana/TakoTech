package moe.takochan.takotech.common.event;

import net.minecraftforge.event.world.WorldEvent;

import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.storage.CellItemSavedData;

public class WorldEventHandler {

    /**
     * @param event 世界加载事件
     */
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (Platform.isServer() && event.world.provider.dimensionId == 0) {
            CellItemSavedData.init(event.world);
            TakoTechMod.LOG.info("StorageCellData initialized successfully!");
        }
    }
}
