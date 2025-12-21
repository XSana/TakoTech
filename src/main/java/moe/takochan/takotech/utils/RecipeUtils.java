package moe.takochan.takotech.utils;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 配方相关的工具方法
 */
public class RecipeUtils {

    private RecipeUtils() {}

    /**
     * 从合成输入中查找带有NBT的物品，并将其NBT复制到结果物品上
     *
     * @param inv    合成网格
     * @param result 合成结果（会被复制）
     * @return 带有复制NBT的结果物品
     */
    public static ItemStack copyNBTFromInput(InventoryCrafting inv, ItemStack result) {
        ItemStack output = result.copy();

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.hasTagCompound()) {
                NBTTagCompound nbt = (NBTTagCompound) stack.getTagCompound()
                    .copy();
                output.setTagCompound(nbt);
                break;
            }
        }

        return output;
    }
}
