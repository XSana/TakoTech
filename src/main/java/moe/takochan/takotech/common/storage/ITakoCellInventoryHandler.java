package moe.takochan.takotech.common.storage;

import appeng.api.storage.ICellInventoryHandler;

public interface ITakoCellInventoryHandler extends ICellInventoryHandler {

    @Override
    ITakoCellInventory getCellInv();
}
