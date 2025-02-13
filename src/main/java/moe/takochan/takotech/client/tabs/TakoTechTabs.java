package moe.takochan.takotech.client.tabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.ModItems;

public class TakoTechTabs extends CreativeTabs {

    public static final TakoTechTabs INSTANCE = new TakoTechTabs(Reference.MODNAME);

    public TakoTechTabs(String name) {
        super(name);
    }

    @Override
    public Item getTabIconItem() {
        return ModItems.ITEM_ORE_STORAGE_CELL;
    }
}
