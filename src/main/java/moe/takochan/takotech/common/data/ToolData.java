package moe.takochan.takotech.common.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;
import moe.takochan.takotech.utils.CommonUtils;

public class ToolData {

    private int slot;

    private ItemStack itemStack;

    public ToolData(int slot, ItemStack itemStack) {
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void writeToByteBuf(ByteBuf buf) {
        buf.writeInt(slot);

        // 将 ItemStack 转为 NBT，并写入 ByteBuf
        NBTTagCompound tag = new NBTTagCompound();
        itemStack.writeToNBT(tag);
        byte[] tagBytes = CommonUtils.nbtToBytes(tag);
        buf.writeInt(tagBytes.length);
        buf.writeBytes(tagBytes);
    }

    public static ToolData readFromByteBuf(ByteBuf buf) {
        int slot = buf.readInt();

        int tagLength = buf.readInt();
        byte[] tagBytes = new byte[tagLength];
        buf.readBytes(tagBytes);
        NBTTagCompound tag = CommonUtils.bytesToNbt(tagBytes);

        ItemStack itemStack = ItemStack.loadItemStackFromNBT(tag);
        return new ToolData(slot, itemStack);
    }
}
