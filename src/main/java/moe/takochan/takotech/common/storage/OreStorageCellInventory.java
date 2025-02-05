package moe.takochan.takotech.common.storage;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import moe.takochan.takotech.common.item.ItemOreStorageCell;
import moe.takochan.takotech.constants.NBTConstants;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import static appeng.me.storage.CellInventory.getCell;

public class OreStorageCellInventory implements IBaseCellInventory {

    // NBT标签名称，用于存储物品类型和数量的标签
    private static final String ITEM_TYPE_TAG = "it";
    private static final String ITEM_COUNT_TAG = "ic";

    // 存储的物品数量和物品类型数量
    private long storedItemCount;
    private long storedItemTypes;

    // 存储单元的物品堆栈、保存提供器和NBT数据
    private final ItemStack storage;
    private final ISaveProvider container;
    private final NBTTagCompound tagCompound;

    // 原件类型实例
    private ItemOreStorageCell cellType;
    // 存储单元中的物品列表
    private IItemList<IAEItemStack> cellItems;

    /**
     * 初始化存储单元的物品堆栈和保存提供器。
     *
     * @param storage   存储单元的物品堆栈
     * @param container 存储单元的保存提供器
     * @throws AppEngException 如果物品堆栈不是有效的存储单元，抛出异常
     */
    public OreStorageCellInventory(ItemStack storage, ISaveProvider container) throws AppEngException {
        if (storage == null) {
            throw new AppEngException("ItemStack was used as a cell, but was not a cell!");
        }

        this.storage = storage;
        this.container = container;

        // 读取NBT数据
        this.tagCompound = Platform.openNbtData(storage);

        // 从NBT数据中读取存储的物品数量和类型
        this.storedItemTypes = tagCompound.getLong(ITEM_TYPE_TAG);
        this.storedItemCount = tagCompound.getLong(ITEM_COUNT_TAG);

        // 获取元件实例
        this.cellType = (ItemOreStorageCell) this.storage.getItem();
    }

    /**
     * 获取当前存储单元的物品堆栈。
     *
     * @return 返回当前存储单元的物品堆栈。
     */
    @Override
    public ItemStack getItemStack() {
        return this.storage;
    }

    /**
     * 获取该存储单元的空闲耗电量。
     *
     * @return 返回该存储单元的空闲状态下的电力消耗
     */
    @Override
    public double getIdleDrain() {
        return this.cellType.getIdleDrain(this.storage);
    }

    /**
     * 获取模糊模式，用于物品匹配。
     *
     * @return 返回模糊模式（用于物品过滤的条件）
     */
    @Override
    public FuzzyMode getFuzzyMode() {
        return this.cellType.getFuzzyMode(this.storage);
    }

    /**
     * 获取配置物品栏的物品库存。
     *
     * @return 返回与该存储单元关联的配置物品栏。
     */
    @Override
    public IInventory getConfigInventory() {
        return this.cellType.getConfigInventory(this.storage);
    }

    /**
     * 获取升级物品栏的物品库存。
     *
     * @return 返回与该存储单元关联的升级物品栏。
     */
    @Override
    public IInventory getUpgradesInventory() {
        return this.cellType.getUpgradesInventory(this.storage);
    }

    /**
     * 获取每种物品所需的字节数。
     *
     * @return 返回每种物品存储所需的字节数。
     */
    @Override
    public int getBytesPerType() {
        return this.cellType.getBytesPerType(this.storage);
    }

    /**
     * 判断该存储单元是否能够存储新的物品。
     *
     * @return 返回是否可以存储新的物品。
     */
    @Override
    public boolean canHoldNewItem() {
        // 获取可用字节数
        final long bytesFree = this.getFreeBytes();

        // 判断存储单元是否有足够空间存储新物品
        return (bytesFree > this.getBytesPerType()
            || (bytesFree == this.getBytesPerType() && this.getUnusedItemCount() > 0))
            && this.getRemainingItemTypes() > 0;
    }

    /**
     * 获取该存储单元的总字节数。
     *
     * @return 返回该存储单元的总字节数。
     */
    @Override
    public long getTotalBytes() {
        return this.cellType.getBytesLong(this.storage);
    }

    /**
     * 获取该存储单元剩余的字节数。
     *
     * @return 返回剩余可用的字节数。
     */
    @Override
    public long getFreeBytes() {
        return this.getTotalBytes() - this.getUsedBytes();
    }

    /**
     * 获取该存储单元已使用的字节数。
     *
     * @return 返回已使用的字节数。
     */
    @Override
    public long getUsedBytes() {
        final long bytesForItemCount = (this.getStoredItemCount() + this.getUnusedItemCount()) / 8;

        return this.getStoredItemTypes() * this.getBytesPerType() + bytesForItemCount;
    }

    /**
     * 获取该存储单元支持的物品种类总数。
     *
     * @return 返回可以存储的物品种类数。
     */
    @Override
    public long getTotalItemTypes() {
        return this.cellType.getTotalTypes(this.storage);
    }

    /**
     * 获取存储单元中当前存储的物品数量。
     *
     * @return 返回存储单元内存储的物品总数量。
     */
    @Override
    public long getStoredItemCount() {
        return this.storedItemCount;
    }

    /**
     * 获取当前存储单元中存储的物品类型数量。
     *
     * @return 返回存储单元中存储的物品类型总数。
     */
    @Override
    public long getStoredItemTypes() {
        return this.storedItemTypes;
    }

    /**
     * 获取存储单元剩余可以存储的物品种类数。
     *
     * @return 返回剩余可以存储的物品种类数量。
     */
    @Override
    public long getRemainingItemTypes() {
        final long basedOnStorage = this.getFreeBytes() / this.getBytesPerType();
        final long baseOnTotal = this.getTotalItemTypes() - this.getStoredItemTypes();

        return Math.min(basedOnStorage, baseOnTotal);
    }

    /**
     * 获取存储单元剩余可以存储的物品数量。
     *
     * @return 返回剩余可以存储的物品总数量。
     */
    @Override
    public long getRemainingItemCount() {
        final long remaining = this.getFreeBytes() * 8 + this.getUnusedItemCount();

        return remaining > 0 ? remaining : 0;
    }

    /**
     * 获取存储单元未使用的物品数量。
     *
     * @return 返回存储单元中未使用的物品数量。
     */
    @Override
    public int getUnusedItemCount() {
        final long div = this.getStoredItemCount() % 8;

        if (div == 0) {
            return 0;
        }

        return (int) (8 - div);
    }

    /**
     * 获取存储单元的状态码。
     *
     * @return 返回存储单元的状态，通常用于表示单元的健康状态或错误代码。
     */
    @Override
    public int getStatusForCell() {
        if (this.canHoldNewItem()) {
            return 1;
        }
        if (this.getRemainingItemCount() > 0) {
            return 2;
        }
        return 3;
    }

    /**
     * 获取该存储单元的矿石过滤器。
     *
     * @return 返回当前的矿石过滤器字符串
     */
    @Override
    public String getOreFilter() {
        return this.cellType.getOreFilter(this.storage);
    }

    /**
     * 向存储单元注入物品。
     *
     * @param input 要注入的物品堆栈。
     * @param mode  注入方式（如完全注入或部分注入）。
     * @param src   执行注入操作的来源。
     * @return 返回成功注入的物品堆栈。
     */
    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        // 检查输入是否为空或物品数量为零，如果是则直接返回输入，不做任何操作
        if (input == null || input.getStackSize() == 0) {
            return null;
        }
        // 检查物品是否在黑名单中，如果是，则不允许注入，直接返回输入
        if (this.cellType.isBlackListed(this.storage, input)) {
            return input;
        }

        // 检查输入物品是否为存储单元
        if (isStorageCell(input)) {
            final IMEInventory<IAEItemStack> meInventory = getCell(input.getItemStack(), null);

            // 如果物品堆栈是有效的存储单元且非空，则直接返回输入物品堆栈
            if (meInventory != null && !this.isEmpty(meInventory)) {
                return input;
            }
        }

        // 查找存储单元中是否已有该物品类型
        final IAEItemStack existingItem = this.getCellItems().findPrecise(input);
        if (existingItem != null) {
            // 获取剩余可用空间
            final long remainingItemSlots = this.getRemainingItemCount();
            // 如果没有剩余空间，返回输入物品堆栈
            if (remainingItemSlots < 0) {
                return input;
            }
            // 如果输入物品数量大于剩余空间，部分注入
            if (input.getStackSize() > remainingItemSlots) {
                final IAEItemStack remainder = input.copy();
                remainder.setStackSize(remainder.getStackSize() - remainingItemSlots);

                if (mode == Actionable.MODULATE) {
                    existingItem.setStackSize(existingItem.getStackSize() + remainingItemSlots);
                    this.updateItemCount(remainingItemSlots);
                    this.saveChanges();
                }

                return remainder;
            } else {
                if (mode == Actionable.MODULATE) {
                    existingItem.setStackSize(existingItem.getStackSize() + input.getStackSize());
                    this.updateItemCount(input.getStackSize());
                    this.saveChanges();
                }

                return null;
            }
        }

        // 如果可以存储新物品类型，则尝试注入新物品
        if (this.canHoldNewItem()) {
            final long remainingItemCount = this.getRemainingItemCount() - this.getBytesPerType() * 8L;

            if (remainingItemCount > 0) {
                // 如果输入物品数量大于剩余可存储空间，则部分注入
                if (input.getStackSize() > remainingItemCount) {
                    final IAEItemStack toReturn = input.copy();
                    toReturn.decStackSize(remainingItemCount);

                    if (mode == Actionable.MODULATE) {
                        final IAEItemStack toWrite = input.copy();
                        toWrite.setStackSize(remainingItemCount);

                        this.cellItems.add(toWrite);
                        this.updateItemCount(toWrite.getStackSize());
                        this.saveChanges();
                    }
                    return toReturn;
                }

                if (mode == Actionable.MODULATE) {
                    this.updateItemCount(input.getStackSize());
                    this.cellItems.add(input);
                    this.saveChanges();
                }

                return null;
            }
        }
        // 如果无法存储新物品，返回输入物品
        return input;
    }

    /**
     * 从存储单元提取物品。
     *
     * @param request 要提取的物品堆栈。
     * @param mode    提取模式（如完全提取或部分提取）。
     * @param src     执行提取操作的来源。
     * @return 返回成功提取的物品堆栈。
     */
    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }

        final long size = request.getStackSize();

        IAEItemStack results = null;

        final IAEItemStack l = this.getCellItems().findPrecise(request);

        if (l != null) {
            results = l.copy();

            if (l.getStackSize() <= size) {
                results.setStackSize(l.getStackSize());

                if (mode == Actionable.MODULATE) {
                    this.updateItemCount(-l.getStackSize());
                    l.setStackSize(0);
                    this.saveChanges();
                }
            } else {
                results.setStackSize(size);

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() - size);
                    this.updateItemCount(-size);
                    this.saveChanges();
                }
            }
        }

        return results;
    }

    /**
     * 获取该存储单元的存储通道。
     *
     * @return 返回该存储单元所属的存储通道。
     */
    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    private IItemList<IAEItemStack> getCellItems() {
        if (this.cellItems == null) {
            this.loadCellItems();
        }

        return this.cellItems;
    }


    /**
     * 判断物品是否为有效的存储单元。
     *
     * @param itemStack 要检查的物品堆栈
     * @return 如果物品堆栈是有效的存储单元，返回 true；否则返回 false
     */
    private static boolean isStorageCell(final IAEItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        try {
            final Item type = itemStack.getItem();

            // 检查物品是否为 IStorageCell 类型，并且是否可以存储其他物品
            if (type instanceof IStorageCell) {
                return !((IStorageCell) type).storableInStorageCell();
            }
        } catch (final Throwable err) {
            return true;
        }

        return false;
    }

    /**
     * 检查给定的物品存储单元是否为空。
     *
     * @param meInventory 物品存储单元的库存实例
     * @return 如果物品存储单元为空，返回 true；否则返回 false
     */
    private boolean isEmpty(final IMEInventory<IAEItemStack> meInventory) {
        boolean isEmpty = meInventory.getAvailableItems(
            AEApi.instance().storage().createItemList(),
            IterationCounter.incrementGlobalDepth()
        ).isEmpty();

        IterationCounter.decrementGlobalDepth();

        return isEmpty;
    }

    /**
     * 更新存储单元的物品数量。
     * 该方法用于增加或减少存储单元中的物品数量，并更新对应的 NBT 标签。
     *
     * @param delta 物品数量的增减值
     */
    private void updateItemCount(final long delta) {
        this.storedItemCount += delta;
        this.tagCompound.setLong(ITEM_COUNT_TAG, this.storedItemCount);
    }

    /**
     * 从存储单元加载物品列表。
     */
    private void loadCellItems() {
        // TODO 初始化读取内容逻辑
        if (this.cellItems == null) {
            this.cellItems = AEApi.instance().storage().createPrimitiveItemList();
        }

        this.cellItems.resetStatus();

        for (final IAEItemStack ais : this.cellItems) {
            ais.setCraftable(false);
            ais.setCountRequestable(0);
        }

    }

    /**
     * 保存物品更改。
     */
    private void saveChanges() {
        // TODO 保存内容逻辑

        if (this.cellItems != null) {
            this.container.saveChanges(this);
        }
    }


    @Override
    public String getDiskID() {
        if (this.tagCompound.hasNoTags()) {
            return "";
        }
        return this.tagCompound.getString(NBTConstants.DISK_ID);
    }
}
