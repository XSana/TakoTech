package moe.takochan.takotech.common.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

public class PlayerDestroyItemEventHandler {

    @SubscribeEvent
    public void onToolBroken(PlayerDestroyItemEvent event) {
        ItemStack brokenStack = event.original;
        if (brokenStack != null && brokenStack.getItem() instanceof MetaGeneratedTool) {
            NBTTagCompound tag = CommonUtils.openNbtData(brokenStack);
            EntityPlayer player = event.entityPlayer;
            if (player != null && tag.hasKey(NBTConstants.TOOLBOX_DATA)) {
                final NBTTagCompound toolboxItems = tag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                final ItemStack toolbox = ItemStack.loadItemStackFromNBT(toolboxItems);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
                if (player instanceof EntityPlayerMP playerMP) {
                    playerMP.sendContainerToPlayer(player.inventoryContainer);
                }
            }
        }
    }

}
