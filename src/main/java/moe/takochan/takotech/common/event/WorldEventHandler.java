package moe.takochan.takotech.common.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.storage.CellItemSavedData;
import moe.takochan.takotech.utils.CommonUtils;

public class WorldEventHandler {

    /**
     * @param event 世界加载事件
     */
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (CommonUtils.isServer() && event.world.provider.dimensionId == 0) {
            CellItemSavedData.init(event.world);
            TakoTechMod.LOG.info("StorageCellData initialized successfully!");
        }
    }

    @SubscribeEvent
    public void onToolBroken(PlayerDestroyItemEvent event) {
        // 在 1.7.10 中，破损的物品通常保存在 event.original 字段中，
        // 而触发事件的玩家在 event.entityPlayer 字段中
        ItemStack brokenStack = event.original;
        if (brokenStack != null && brokenStack.getItem() instanceof MetaGeneratedTool) {
            EntityPlayer player = event.entityPlayer;

        }
    }
}
