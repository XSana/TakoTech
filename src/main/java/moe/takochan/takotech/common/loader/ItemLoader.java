package moe.takochan.takotech.common.loader;

import moe.takochan.takotech.common.item.ItemToolboxPlus;
import moe.takochan.takotech.common.item.ae.ItemOreStorageCell;

/**
 * 物品注册
 */
public class ItemLoader implements Runnable {

    public static ItemOreStorageCell ITEM_ORE_STORAGE_CELL;

    public static ItemToolboxPlus ITEM_TOOLBOX_PLUS;

    public ItemLoader() {
        ITEM_ORE_STORAGE_CELL = new ItemOreStorageCell();

        ITEM_TOOLBOX_PLUS = new ItemToolboxPlus();
    }

    @Override
    public void run() {
        registerItems();
    }

    private void registerItems() {
        ITEM_ORE_STORAGE_CELL.register();

        ITEM_TOOLBOX_PLUS.register();
    }
}
