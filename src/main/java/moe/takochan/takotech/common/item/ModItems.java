package moe.takochan.takotech.common.item;

public final class ModItems {

    public static final ItemMineStorageCell itemMineStorageCell = new ItemMineStorageCell();

    public static void registerItems() {
        itemMineStorageCell.register();
    }
}
