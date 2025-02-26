package moe.takochan.takotech.client.tabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.loader.BlockLoader;

public class TakoTechTabs extends CreativeTabs {

    private static final TakoTechTabs INSTANCE = new TakoTechTabs(Reference.MODNAME);

    public TakoTechTabs(String name) {
        super(name);
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(BlockLoader.BLOCK_WEB_CONTROLLER);
    }

    public static TakoTechTabs getInstance() {
        return INSTANCE;
    }
}
