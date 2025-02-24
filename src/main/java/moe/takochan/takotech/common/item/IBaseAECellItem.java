package moe.takochan.takotech.common.item;

import net.minecraft.item.ItemStack;

import appeng.api.exceptions.AppEngException;
import appeng.api.storage.ISaveProvider;
import moe.takochan.takotech.common.storage.ITakoCellInventory;

public interface IBaseAECellItem extends IBaseItem {

    /**
     * 获取库存管理实例
     *
     * @param o         物品堆栈
     * @param container 存储提供者，用于保存和管理数据
     * @return 库存管理实例
     * @throws AppEngException 如果无法获取库存或发生错误时抛出该异常
     */
    ITakoCellInventory getCellInv(ItemStack o, ISaveProvider container) throws AppEngException;
}
