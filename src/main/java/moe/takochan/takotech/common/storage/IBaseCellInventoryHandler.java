package moe.takochan.takotech.common.storage;

import appeng.api.storage.ICellInventoryHandler;

public interface IBaseCellInventoryHandler extends ICellInventoryHandler {

    @Override
    IBaseCellInventory getCellInv();

}
