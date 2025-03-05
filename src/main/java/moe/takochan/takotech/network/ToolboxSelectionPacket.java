package moe.takochan.takotech.network;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ToolboxSelectionPacket implements IMessage {

    private ItemStack itemStack;

    public ToolboxSelectionPacket() {}

    public ToolboxSelectionPacket(ItemStack itemStack) {
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
}
