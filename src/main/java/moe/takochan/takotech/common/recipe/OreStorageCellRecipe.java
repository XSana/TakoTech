package moe.takochan.takotech.common.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import moe.takochan.takotech.utils.RecipeUtils;

/**
 * 矿物存储元件配方，支持在元件类型转换时保留NBT数据
 */
public class OreStorageCellRecipe extends ShapedOreRecipe {

    public OreStorageCellRecipe(ItemStack result, Object... recipe) {
        super(result, recipe);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return RecipeUtils.copyNBTFromInput(inv, this.getRecipeOutput());
    }
}
