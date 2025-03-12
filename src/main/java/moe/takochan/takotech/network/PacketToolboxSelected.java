package moe.takochan.takotech.network;

import static moe.takochan.takotech.common.item.ic2.ItemToolboxPlus.getToolbox;
import static moe.takochan.takotech.common.item.ic2.ItemToolboxPlus.isMetaGeneratedTool;
import static moe.takochan.takotech.common.item.ic2.ItemToolboxPlus.isToolboxPlus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

public class PacketToolboxSelected implements IMessage {

    private int slot;

    public PacketToolboxSelected() {}

    public PacketToolboxSelected(int slot) {
        this.slot = slot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slot = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slot);
    }

    public static class Handler implements IMessageHandler<PacketToolboxSelected, IMessage> {

        private EntityPlayer player;
        private ItemStack currentItem;

        @Override
        public IMessage onMessage(PacketToolboxSelected message, MessageContext ctx) {
            // ToolData.setSelectedSlot(message.slot);
            player = ctx.getServerHandler().playerEntity;
            currentItem = player.inventory.getCurrentItem();
            selectTool(message.slot);
            return null;
        }

        public void selectTool(int slot) {
            getToolbox(currentItem).ifPresent(t -> {
                ItemStack toolbox = t.copy();
                if (toolbox.getItem() instanceof ItemToolboxPlus) {
                    if (isToolboxPlus(currentItem)) {
                        handleToolSelection(slot, toolbox);
                    } else if (isMetaGeneratedTool(currentItem)) {
                        handleMetaToolSelection(slot, toolbox);
                    }
                }
            });
        }

        private void handleToolSelection(int slot, ItemStack toolbox) {
            ItemStack tool = ItemToolboxPlus.getStackInSlot(toolbox, slot);
            if (tool == null) return;
            ItemToolboxPlus.setItemToSlot(toolbox, slot, null);
            NBTTagCompound toolNbt = new NBTTagCompound();
            toolbox.writeToNBT(toolNbt);
            NBTTagCompound nbt = CommonUtils.openNbtData(tool);
            nbt.setTag(NBTConstants.TOOLBOX_DATA, toolNbt);
            nbt.setInteger(NBTConstants.TOOLBOX_SLOT, slot);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, tool);
        }

        private void handleMetaToolSelection(int slot, ItemStack toolbox) {
            NBTTagCompound nbt = CommonUtils.openNbtData(currentItem);
            int currentSlot = nbt.getInteger(NBTConstants.TOOLBOX_SLOT);
            nbt.removeTag(NBTConstants.TOOLBOX_DATA);
            nbt.removeTag(NBTConstants.TOOLBOX_SLOT);
            ItemToolboxPlus.setItemToSlot(toolbox, currentSlot, currentItem);
            if (slot == -1) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
            } else {
                handleToolSelection(slot, toolbox);
            }
        }
    }
}
