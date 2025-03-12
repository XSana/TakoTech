package moe.takochan.takotech.common.data;

import net.minecraft.item.ItemStack;

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
}
