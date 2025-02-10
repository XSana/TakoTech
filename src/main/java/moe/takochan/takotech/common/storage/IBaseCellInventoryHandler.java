package moe.takochan.takotech.common.storage;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEItemStack;

public interface IBaseCellInventoryHandler extends IMEInventoryHandler<IAEItemStack> {

    IBaseCellInventory getCellInv();


    boolean isPreformatted();

    boolean isFuzzy();

    IncludeExclude getIncludeExcludeMode();

}
