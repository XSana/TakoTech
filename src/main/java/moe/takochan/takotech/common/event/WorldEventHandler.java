package moe.takochan.takotech.common.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.common.storage.CellItemSavedData;
import moe.takochan.takotech.constants.NBTConstants;
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
        // 破损的物品通常保存在 event.original 字段中，
        // 而触发事件的玩家在 event.entityPlayer 字段中
        ItemStack brokenStack = event.original;
        if (brokenStack != null && brokenStack.getItem() instanceof MetaGeneratedTool) {
            NBTTagCompound tag = CommonUtils.openNbtData(brokenStack);
            EntityPlayer player = event.entityPlayer;
            if (player != null && tag.hasKey(NBTConstants.TOOLBOX_DATA)) {
                final NBTTagList toolboxItems = tag.getTagList(NBTConstants.TOOLBOX_DATA, Constants.NBT.TAG_COMPOUND);

                // 创建新的工具箱物品
                final ItemStack toolbox = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);
                final NBTTagCompound newTag = CommonUtils.openNbtData(toolbox);

                // 清理选择状态后保存数据
                removeSelectionTags(toolboxItems);
                newTag.setTag(NBTConstants.TOOLBOX_ITEMS, toolboxItems);
                player.inventory.mainInventory[player.inventory.currentItem] = toolbox;
            }
        }
    }

    private void removeSelectionTags(NBTTagList toolboxItems) {
        // 倒序遍历避免索引错位
        for (int i = toolboxItems.tagCount() - 1; i >= 0; i--) {
            final NBTTagCompound toolTag = toolboxItems.getCompoundTagAt(i);
            if (toolTag.hasKey(NBTConstants.TOOLBOX_SELECTED)) {
                toolTag.removeTag(NBTConstants.TOOLBOX_SELECTED);
            }
        }
    }
}
