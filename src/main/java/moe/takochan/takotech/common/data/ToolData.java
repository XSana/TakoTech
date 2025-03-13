package moe.takochan.takotech.common.data;

import net.minecraft.item.ItemStack;

public class ToolData {

    // 工具所在的槽位
    private int slot;
    // 工具的物品堆栈
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
