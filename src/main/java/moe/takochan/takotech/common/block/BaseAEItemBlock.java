package moe.takochan.takotech.common.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import appeng.block.AEBaseItemBlock;

/**
 * AE模组物品块的抽象基类
 */
public abstract class BaseAEItemBlock extends AEBaseItemBlock {

    /**
     * @param block 关联的AE方块对象
     */
    public BaseAEItemBlock(Block block) {
        super(block);
    }

    /**
     * 获取国际化名称（未本地化版本）
     *
     * @param itemStack 物品堆栈（用于多状态处理）
     */
    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return super.getUnlocalizedName(itemStack);
    }
}
