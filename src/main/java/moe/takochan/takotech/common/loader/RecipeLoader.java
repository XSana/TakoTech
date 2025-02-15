package moe.takochan.takotech.common.loader;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import appeng.api.AEApi;
import appeng.core.ApiDefinitions;
import appeng.core.api.definitions.ApiMaterials;
import cpw.mods.fml.common.registry.GameRegistry;

public class RecipeLoader {

    private static ItemStack BLOCK_COBBLESTONE;

    private static ItemStack ITEM_AE2_EMPTY_STORAGE_CELL;

    private static ItemStack ITEM_AE2_CELL_64_PART;

    public static void init() {

        ApiDefinitions aeDef = (ApiDefinitions) AEApi.instance()
            .definitions();
        ApiMaterials aeMaterials = aeDef.materials();

        BLOCK_COBBLESTONE = new ItemStack(Blocks.cobblestone);

        ITEM_AE2_EMPTY_STORAGE_CELL = aeMaterials.emptyStorageCell()
            .maybeStack(1)
            .get();

        ITEM_AE2_CELL_64_PART = aeMaterials.cell64kPart()
            .maybeStack(1)
            .get();
    }

    public static void registryRecipe() {

        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(ItemLoader.ITEM_ORE_STORAGE_CELL, 1),
                "CDC",
                "DED",
                "CDC",
                'C',
                ITEM_AE2_EMPTY_STORAGE_CELL,
                'D',
                BLOCK_COBBLESTONE,
                'E',
                ITEM_AE2_CELL_64_PART));
    }
}
