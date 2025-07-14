package moe.takochan.takotech.common.loader;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import appeng.api.AEApi;
import appeng.core.ApiDefinitions;
import appeng.core.api.definitions.ApiMaterials;
import cpw.mods.fml.common.registry.GameRegistry;
import ic2.core.Ic2Items;
import moe.takochan.takotech.common.item.ae.OreStorageType;
import moe.takochan.takotech.common.recipe.OreStorageCellRecipe;
import moe.takochan.takotech.common.recipe.ToolboxPlusRecipe;

/**
 * 配方注册
 */
public class RecipeLoader implements Runnable {

    private static ItemStack BLOCK_COBBLESTONE;

    private static ItemStack ITEM_AE2_EMPTY_STORAGE_CELL;

    private static ItemStack ITEM_AE2_CELL_64_PART;

    private static ItemStack ITEM_IC2_TOOLBOX;

    public RecipeLoader() {
        ApiDefinitions aeDef = (ApiDefinitions) AEApi.instance()
            .definitions();
        ApiMaterials aeMaterials = aeDef.materials();

        BLOCK_COBBLESTONE = new ItemStack(Blocks.cobblestone);

        ITEM_AE2_EMPTY_STORAGE_CELL = aeMaterials.emptyStorageCell()
            .maybeStack(1)
            .orNull();

        ITEM_AE2_CELL_64_PART = aeMaterials.cell64kPart()
            .maybeStack(1)
            .orNull();

        ITEM_IC2_TOOLBOX = Ic2Items.toolbox;
    }

    @Override
    public void run() {
        registryRecipe();
    }

    private void registryRecipe() {
        // 矿物存储元件
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(ItemLoader.ITEM_ORE_STORAGE_CELL, 1, 0),
                "CDC",
                "DED",
                "CDC",
                'C',
                ITEM_AE2_EMPTY_STORAGE_CELL,
                'D',
                BLOCK_COBBLESTONE,
                'E',
                ITEM_AE2_CELL_64_PART));

        for (OreStorageType type : OreStorageType.values()) {
            int meta = type.getMeta(); // 0~6

            int x = meta % 3;
            int y = meta / 3;

            String[] pattern = { "   ", "   ", "   " };
            StringBuilder row = new StringBuilder(pattern[y]);
            row.setCharAt(x, 'C');
            pattern[y] = row.toString();

            GameRegistry.addRecipe(
                new OreStorageCellRecipe(
                    new ItemStack(ItemLoader.ITEM_ORE_STORAGE_CELL, 1, meta),
                    pattern[0],
                    pattern[1],
                    pattern[2],
                    'C',
                    new ItemStack(ItemLoader.ITEM_ORE_STORAGE_CELL, 1, 32767)));
        }

        GameRegistry.addRecipe(new ToolboxPlusRecipe(new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS, 1), ITEM_IC2_TOOLBOX));
    }

}
