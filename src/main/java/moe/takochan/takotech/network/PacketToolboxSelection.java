package moe.takochan.takotech.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gregtech.api.items.MetaGeneratedTool;
import io.netty.buffer.ByteBuf;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;

public class PacketToolboxSelection implements IMessage {

    private ItemStack itemStack;

    public PacketToolboxSelection() {
    }

    public PacketToolboxSelection(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        itemStack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, itemStack);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public static class Handler implements IMessageHandler<PacketToolboxSelection, IMessage> {

        @Override
        public IMessage onMessage(PacketToolboxSelection message, MessageContext ctx) {
            // 获取玩家实体（1.7.10专用方式）
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            // 在主服务端线程执行
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null) {
                if (heldItem.getItem() instanceof ItemToolboxPlus || heldItem.getItem() instanceof MetaGeneratedTool) {
                    ItemToolboxPlus.processSelection(player, message.getItemStack());
                }
            }

            return null;
        }
    }
}
