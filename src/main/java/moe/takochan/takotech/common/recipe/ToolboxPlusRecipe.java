package moe.takochan.takotech.common.recipe;

import java.util.Collections;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;

import moe.takochan.takotech.utils.CommonUtils;

public class ToolboxPlusRecipe extends ShapelessRecipes {

    public ToolboxPlusRecipe(ItemStack output, ItemStack input) {
        super(output, Collections.singletonList(input));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = this.getRecipeOutput()
            .copy(); // 复制合成产物
        NBTTagCompound nbt = null;

        // 遍历合成槽，找到唯一的输入物品
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.hasTagCompound()) {
                nbt = (NBTTagCompound) CommonUtils.openNbtData(stack)
                    .copy();
                break;
            }
        }

        // 如果找到 NBT，则复制到合成结果中
        if (nbt != null) {
            result.setTagCompound(nbt);
        }
        return result;
    }
}
