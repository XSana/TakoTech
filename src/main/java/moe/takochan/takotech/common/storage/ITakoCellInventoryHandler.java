package moe.takochan.takotech.common.storage;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEItemStack;

public interface ITakoCellInventoryHandler extends IMEInventoryHandler<IAEItemStack> {

    ITakoCellInventory getCellInv();


    boolean isPreformatted();

    boolean isFuzzy();

    IncludeExclude getIncludeExcludeMode();

}
