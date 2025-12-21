package moe.takochan.takotech.common.recipe;

import java.util.Collections;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;

import moe.takochan.takotech.utils.RecipeUtils;

/**
 * 高级工具箱配方，支持从普通工具箱升级时保留NBT数据
 */
public class ToolboxPlusRecipe extends ShapelessRecipes {

    public ToolboxPlusRecipe(ItemStack output, ItemStack input) {
        super(output, Collections.singletonList(input));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return RecipeUtils.copyNBTFromInput(inv, this.getRecipeOutput());
    }
}
