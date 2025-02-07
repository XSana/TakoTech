package moe.takochan.takotech.common.storage;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.ICellCacheRegistry;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.MEPassThrough;
import appeng.util.item.AEItemStack;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.OreFilteredList;
import appeng.util.prioitylist.PrecisePriorityList;
import com.glodblock.github.util.Ae2Reflect;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class CellInventoryHandler
    extends MEInventoryHandler<IAEItemStack>
    implements IBaseCellInventoryHandler, ICellCacheRegistry {

    public CellInventoryHandler(IMEInventory<IAEItemStack> meInventory) {
        super(meInventory, StorageChannel.ITEMS);
        final IBaseCellInventory ci = this.getCellInv();
        if (ci != null) {
            final IInventory upgrades = ci.getUpgradesInventory();
            final IInventory config = ci.getConfigInventory();
            final FuzzyMode fzMode = ci.getFuzzyMode();
            final String filter = ci.getOreFilter();

            boolean hasInverter = false;
            boolean hasFuzzy = false;
            boolean hasOreFilter = false;
            boolean hasSticky = false;

            for (int i = 0; i < upgrades.getSizeInventory(); i++) {
                final ItemStack is = upgrades.getStackInSlot(i);
                if (is != null && is.getItem() instanceof IUpgradeModule) {
                    final Upgrades u = ((IUpgradeModule) is.getItem()).getType(is);
                    if (u != null) {
                        switch (u) {
                            case FUZZY -> hasFuzzy = true;
                            case INVERTER -> hasInverter = true;
                            case ORE_FILTER -> hasOreFilter = true;
                            case STICKY -> hasSticky = true;
                            default -> {
                            }
                        }
                    }
                }
            }
            this.setWhitelist(hasInverter ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);

            if (hasSticky) {
                setSticky(true);
            }

            if (hasOreFilter && !filter.isEmpty()) {
                this.setPartitionList(new OreFilteredList(filter));
            } else {
                final IItemList<IAEItemStack> priorityList = AEApi.instance().storage().createItemList();
                for (int i = 0; i < config.getSizeInventory(); i++) {
                    final ItemStack is = config.getStackInSlot(i);
                    if (is != null) {
                        priorityList.add(AEItemStack.create(is));
                    }
                }
                if (!priorityList.isEmpty()) {
                    if (hasFuzzy) {
                        this.setPartitionList(new FuzzyPriorityList<>(priorityList, fzMode));
                    } else {
                        this.setPartitionList(new PrecisePriorityList<>(priorityList));
                    }
                }
            }
        }
    }

    @Override
    public IBaseCellInventory getCellInv() {
        Object o = this.getInternal();
        if (o instanceof MEPassThrough) {
            o = Ae2Reflect.getInternal((MEPassThrough<?>) o);
        }
        return (IBaseCellInventory) (o instanceof IBaseCellInventory ? o : null);
    }

    @Override
    public boolean isPreformatted() {
        return !Ae2Reflect.getPartitionList(this).isEmpty();
    }

    @Override
    public boolean isFuzzy() {
        return Ae2Reflect.getPartitionList(this) instanceof FuzzyPriorityList;
    }

    @Override
    public IncludeExclude getIncludeExcludeMode() {
        return this.getWhitelist();
    }

    @Override
    public boolean canGetInv() {
        return this.getCellInv() != null;
    }

    @Override
    public long getTotalBytes() {
        return this.getCellInv().getTotalBytes();
    }

    @Override
    public long getFreeBytes() {
        return this.getCellInv().getFreeBytes();
    }

    @Override
    public long getUsedBytes() {
        return this.getCellInv().getUsedBytes();
    }

    @Override
    public long getTotalTypes() {
        return this.getCellInv().getTotalItemTypes();
    }

    @Override
    public long getFreeTypes() {
        return this.getCellInv().getRemainingItemTypes();
    }

    @Override
    public long getUsedTypes() {
        return this.getCellInv().getStoredItemTypes();
    }

    @Override
    public int getCellStatus() {
        return this.getStatusForCell();
    }


    @Override
    public TYPE getCellType() {
        return TYPE.ITEM;
    }

    public int getStatusForCell() {
        int val = this.getCellInv().getStatusForCell();

        if ((val == 1 || val == 2) && this.isPreformatted()) {
            val = 3;
        }

        return val;
    }
}
