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

        /**
         * 处理工具选择逻辑
         *
         * @param slot 选择的槽位
         */
        public void selectTool(int slot) {
            // 获取工具箱并处理选择逻辑
            getToolbox(currentItem).ifPresent(t -> {
                // 复制工具箱
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

        /**
         * 处理高级工具箱的工具选择逻辑
         *
         * @param slot    选择的槽位
         * @param toolbox 工具箱物品
         */
        private void handleToolSelection(int slot, ItemStack toolbox) {
            // 获取指定槽位的工具
            ItemStack tool = ItemToolboxPlus.getStackInSlot(toolbox, slot);
            // 如果工具为空，直接返回
            if (tool == null) return;

            // 将指定槽位的工具设置为空
            ItemToolboxPlus.setItemToSlot(toolbox, slot, null);

            // 将工具箱数据写入工具的 NBT
            NBTTagCompound toolNbt = new NBTTagCompound();
            toolbox.writeToNBT(toolNbt);
            NBTTagCompound nbt = CommonUtils.openNbtData(tool);
            nbt.setTag(NBTConstants.TOOLBOX_DATA, toolNbt);
            nbt.setInteger(NBTConstants.TOOLBOX_SLOT, slot);

            // 将工具放入玩家当前手持的槽位
            player.inventory.setInventorySlotContents(player.inventory.currentItem, tool);
        }

        /**
         * 处理 MetaGeneratedTool 的工具选择逻辑
         *
         * @param slot    选择的槽位
         * @param toolbox 工具箱物品
         */
        private void handleMetaToolSelection(int slot, ItemStack toolbox) {
            // 获取当前物品的 NBT 数据
            NBTTagCompound nbt = CommonUtils.openNbtData(currentItem);
            int currentSlot = nbt.getInteger(NBTConstants.TOOLBOX_SLOT);

            // 移除工具箱数据和槽位数据
            nbt.removeTag(NBTConstants.TOOLBOX_DATA);
            nbt.removeTag(NBTConstants.TOOLBOX_SLOT);

            // 将当前物品放回工具箱的指定槽位
            ItemToolboxPlus.setItemToSlot(toolbox, currentSlot, currentItem);
            if (slot == -1) {
                // 如果选择的是默认槽位，则将工具箱放入玩家当前手持的槽位
                player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
            } else {
                // 否则，处理高级工具箱的工具选择逻辑
                handleToolSelection(slot, toolbox);
            }
        }
    }
}
