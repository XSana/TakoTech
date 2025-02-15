package moe.takochan.takotech.common.loader;

import moe.takochan.takotech.common.item.ae.ItemOreStorageCell;

public final class ItemLoader {

    public static final ItemOreStorageCell ITEM_ORE_STORAGE_CELL = new ItemOreStorageCell();

    public static void registerItems() {
        ITEM_ORE_STORAGE_CELL.register();
    }
}
