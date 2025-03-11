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
import ic2.core.IHasGui;
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
            return null;
        }

        public void selectTool(int slot, EntityPlayer player) {
            getToolbox(currentItem).ifPresent(toolbox -> {
                if (toolbox.getItem() instanceof ItemToolboxPlus toolboxPlus) {
                    final IHasGui toolboxGUI = toolboxPlus.getInventory(player, toolbox);
                    if (isToolboxPlus(currentItem)) {
                        handleToolSelection(toolboxGUI, slot, toolbox);
                    } else if (isMetaGeneratedTool(currentItem)) {
                        handleMetaToolSelection(toolboxGUI, slot, toolbox);
                    }
                }
            });
        }

        private void handleToolSelection(IHasGui toolboxGUI, int slot, ItemStack toolbox) {
            ItemStack tool = toolboxGUI.getStackInSlot(slot)
                .copy();
            toolboxGUI.setInventorySlotContents(slot, null);
            NBTTagCompound toolNbt = new NBTTagCompound();
            toolbox.writeToNBT(toolNbt);
            CommonUtils.openNbtData(tool)
                .setTag(NBTConstants.TOOLBOX_DATA, toolNbt);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, tool);
        }

        private void handleMetaToolSelection(IHasGui toolboxGUI, int slot, ItemStack toolbox) {
            NBTTagCompound nbt = CommonUtils.openNbtData(currentItem);
            int currentSlot = nbt.getInteger(NBTConstants.TOOLBOX_TOOLS_SLOT);
            nbt.removeTag(NBTConstants.TOOLBOX_DATA);
            nbt.removeTag(NBTConstants.TOOLBOX_TOOLS_SLOT);
            toolboxGUI.setInventorySlotContents(currentSlot, currentItem);
            if (slot == -1) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
            } else {
                handleToolSelection(toolboxGUI, slot, toolbox);
            }
        }
    }
}
