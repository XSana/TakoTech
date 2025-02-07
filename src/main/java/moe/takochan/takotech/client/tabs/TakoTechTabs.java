package moe.takochan.takotech.common.tabs;

import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

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
