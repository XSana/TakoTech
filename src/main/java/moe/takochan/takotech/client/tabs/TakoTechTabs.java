package moe.takochan.takotech.client.tabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.loader.BlockLoader;

/**
 * 创造模式物品栏标签页
 */
public class TakoTechTabs extends CreativeTabs {

    /**
     * 单例实例
     */
    private static final TakoTechTabs INSTANCE = new TakoTechTabs(Reference.MODNAME);

    /**
     * 构造创造模式标签页
     *
     * @param name 标签页名称，通常使用Mod名称
     */
    public TakoTechTabs(String name) {
        super(name);
    }

    /**
     * 设置标签页图标为指定的方块物品
     */
    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(BlockLoader.BLOCK_WEB_CONTROLLER);
    }

    /**
     * 获取单例实例
     */
    public static TakoTechTabs getInstance() {
        return INSTANCE;
    }
}
