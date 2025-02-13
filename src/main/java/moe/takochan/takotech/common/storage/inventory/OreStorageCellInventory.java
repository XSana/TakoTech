package moe.takochan.takotech.common.storage.inventory;

import static appeng.me.storage.CellInventory.getCell;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

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
import moe.takochan.takotech.common.item.ae.ItemOreStorageCell;
import moe.takochan.takotech.common.storage.CellItemSavedData;
import moe.takochan.takotech.common.storage.CellItemStorage;
import moe.takochan.takotech.common.storage.ITakoCellInventory;
import moe.takochan.takotech.constants.NBTConstants;

/**
 * 矿物存储元件库存管理。
 */
public class OreStorageCellInventory implements ITakoCellInventory {

    // NBT标签名称，用于存储物品类型和数量的标签
    private static final String ITEM_TYPE_TAG = "it";
    private static final String ITEM_COUNT_TAG = "ic";

    // 存储的物品数量和物品类型数量
    private final long storedItemCount;
    private int storedItemTypes;

    // 元件的物品堆栈、保存提供器和NBT数据
    private final ItemStack cellItem;
    private final ISaveProvider container;
    private final NBTTagCompound tagCompound;

    // 原件类型实例
    private final ItemOreStorageCell cellType;
    // 元件中的物品列表
    private IItemList<IAEItemStack> cellItems;

    // 元件的数据存储实例
    protected final CellItemStorage storage;

    /**
     * 初始化元件的物品堆栈和保存提供器。
     *
     * @param cellItem  元件的物品堆栈
     * @param container 元件的保存提供器
     * @throws AppEngException 如果物品堆栈不是有效的元件，抛出异常
     */
    public OreStorageCellInventory(ItemStack cellItem, ISaveProvider container) throws AppEngException {
        if (cellItem == null) {
            throw new AppEngException("ItemStack was used as a cell, but was not a cell!");
        }

        // 获取物品堆栈关联的元件库存处理器
        this.cellItem = cellItem;
        this.container = container;

        // 读取NBT数据
        this.tagCompound = Platform.openNbtData(cellItem);

        // 从NBT数据中读取存储的物品数量和类型
        this.storedItemTypes = tagCompound.getInteger(ITEM_TYPE_TAG);
        this.storedItemCount = tagCompound.getLong(ITEM_COUNT_TAG);

        // 获取元件实例
        this.cellType = (ItemOreStorageCell) this.cellItem.getItem();

        // 获取元件的数据存储实例
        this.storage = CellItemSavedData.getInstance()
            .getDataStorage(this.getItemStack());
    }

    /**
     * 获取当前元件的物品堆栈。
     *
     * @return 返回当前元件的物品堆栈。
     */
    @Override
    public ItemStack getItemStack() {
        return this.cellItem;
    }

    /**
     * 获取该元件的空闲耗电量。
     *
     * @return 返回该元件的空闲状态下的电力消耗
     */
    @Override
    public double getIdleDrain() {
        return this.cellType.getIdleDrain(this.cellItem);
    }

    /**
     * 获取模糊模式，用于物品匹配。
     *
     * @return 返回模糊模式（用于物品过滤的条件）
     */
    @Override
    public FuzzyMode getFuzzyMode() {
        return this.cellType.getFuzzyMode(this.cellItem);
    }

    /**
     * 获取配置列表
     *
     * @return 返回配置列表。
     */
    @Override
    public IInventory getConfigInventory() {
        return this.cellType.getConfigInventory(this.cellItem);
    }

    /**
     * 获取已安装的升级。
     *
     * @return 返回已安装的升级 。
     */
    @Override
    public IInventory getUpgradesInventory() {
        return this.cellType.getUpgradesInventory(this.cellItem);
    }

    /**
     * 获取每种物品所需的字节数。
     *
     * @return 返回每种物品存储所需的字节数。
     */
    @Override
    public int getBytesPerType() {
        return this.cellType.getBytesPerType(this.cellItem);
    }

    /**
     * 判断该元件是否能够存储新的物品。
     *
     * @return 返回是否可以存储新的物品。
     */
    @Override
    public boolean canHoldNewItem() {
        return this.getStoredItemTypes() <= this.getTotalItemTypes();
    }

    /**
     * 获取该元件的总字节数。
     *
     * @return 返回该元件的总字节数。
     */
    @Override
    public long getTotalBytes() {
        return this.cellType.getBytesLong(this.cellItem);
    }

    /**
     * 获取该元件剩余的字节数。
     *
     * @return 返回剩余可用的字节数。
     */
    @Override
    public long getFreeBytes() {
        // 恒定为最大值，实现无限存储
        return Long.MAX_VALUE;
    }

    /**
     * 获取该元件已使用的字节数。
     *
     * @return 返回已使用的字节数。
     */
    @Override
    public long getUsedBytes() {
        // 恒定为0，实现无限存储
        return 0;
    }

    /**
     * 获取该元件最大可存储物品类型数。
     *
     * @return 返回最大可存储物品类型数。
     */
    @Override
    public long getTotalItemTypes() {
        return this.cellType.getTotalTypes(this.cellItem);
    }

    /**
     * 获取元件中当前存储的物品数量。
     *
     * @return 返回元件内存储的物品总数量。
     */
    @Override
    public long getStoredItemCount() {
        // 理论上恒定为0
        return this.storedItemCount;
    }

    /**
     * 获取当前元件中存储的物品类型数量。
     *
     * @return 返回元件中存储的物品类型总数。
     */
    @Override
    public long getStoredItemTypes() {
        return this.storedItemTypes;
    }

    /**
     * 获取元件剩余可以存储的物品种类数。
     *
     * @return 返回剩余可以存储的物品种类数量。
     */
    @Override
    public long getRemainingItemTypes() {
        return this.getTotalItemTypes() - this.getStoredItemTypes();
    }

    /**
     * 获取元件剩余可以存储的物品数量。
     *
     * @return 返回剩余可以存储的物品总数量。
     */
    @Override
    public long getRemainingItemCount() {
        // 理论上恒定为最大值，实现无限存储
        return Long.MAX_VALUE;
    }

    /**
     * 获取元件未使用的物品数量。
     *
     * @return 返回元件中未使用的物品数量。
     */
    @Override
    public int getUnusedItemCount() {
        // 理论上恒定为最大值，实现无限存储
        return Integer.MAX_VALUE;
    }

    /**
     * 获取元件的状态码。
     *
     * @return 返回元件的状态，通常用于驱动器展示单元的状态。
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
     * 获取该元件的矿石过滤器。
     *
     * @return 返回当前的矿石过滤器字符串
     */
    @Override
    public String getOreFilter() {
        return this.cellType.getOreFilter(this.cellItem);
    }

    /**
     * 向元件注入物品。
     *
     * @param input 要注入的物品堆栈。
     * @param mode  注入方式（如完全注入或部分注入）。
     * @param src   执行注入操作的来源。
     * @return 返回无法注入物品
     */
    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        // 检查输入是否为空或物品数量为零，如果是则直接返回输入，不做任何操作
        if (input == null || input.getStackSize() == 0) {
            return null;
        }
        // 检查物品是否在黑名单中，如果是，则不允许注入，直接返回输入
        if (this.cellType.isBlackListed(this.cellItem, input)) {
            return input;
        }

        // 检查输入物品是否为元件
        if (isStorageCell(input)) {
            final IMEInventory<IAEItemStack> meInventory = getCell(input.getItemStack(), null);

            // 如果物品堆栈是有效的元件且非空，则直接返回输入物品堆栈
            if (meInventory != null && !this.isEmpty(meInventory)) {
                return input;
            }
        }

        // 确保注入的物品数量大于0
        if (input.getStackSize() > 0) {
            // 查找元件中是否已有该物品类型
            final IAEItemStack existingItem = this.getCellItems()
                .findPrecise(input);
            // 如果元件中已有该物品类型，则更新该物品数量
            if (existingItem != null && mode == Actionable.MODULATE) {
                existingItem.setStackSize(existingItem.getStackSize() + input.getStackSize());
                this.saveChanges();
            }

            // 如果元件中无该物品类型，并且元件中还有剩余空间，则添加该物品类型
            if (this.canHoldNewItem() && mode == Actionable.MODULATE) {
                // 确保注入的物品数量大于0
                this.cellItems.add(input);
                this.saveChanges();
            }
            return null;
        }
        // 如果无法存储新物品，返回输入物品
        return input;
    }

    /**
     * 从元件提取物品。
     *
     * @param request 要提取的物品堆栈，包含物品类型和数量。
     * @param mode    提取模式，定义了物品提取的方式（如完全提取或部分提取）。
     * @param src     执行提取操作的来源，可以是用户操作或系统请求。
     * @return 返回成功提取的物品堆栈。如果没有物品可提取，则返回 `null`。
     */
    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }

        // 获取要提取的物品数量
        final long size = request.getStackSize();

        // 创建提取物品堆栈
        IAEItemStack results = null;

        // 查找元件中是否已有该物品类型
        final IAEItemStack l = this.getCellItems()
            .findPrecise(request);

        // 如果物品存在
        if (l != null) {
            // 复制物品堆栈，准备返回提取结果
            results = l.copy();

            // 如果元件中的物品数量小于或等于要提取的数量
            if (l.getStackSize() <= size) {
                // 设置提取结果的堆栈数量为元件中现有物品的数量
                results.setStackSize(l.getStackSize());

                // 清空元件中该物品
                if (mode == Actionable.MODULATE) {
                    l.setStackSize(0);
                    this.saveChanges();
                }
            } else {
                // 设置提取结果的堆栈数量为要提取的数量
                results.setStackSize(size);

                // 从元件中减去提取的物品数量
                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() - size);
                    this.saveChanges();
                }
            }
        }

        return results;
    }

    /**
     * 获取该元件的存储通道。
     *
     * @return 返回该元件所属的存储通道。
     */
    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    /**
     * 获取可用的物品堆栈。
     *
     * @param request   请求的物品堆栈，包含物品类型和数量。
     * @param iteration 当前迭代次数，通常用于深度控制或递归限制。
     * @return 返回找到的物品副本，如果没有找到该物品，则返回 `null`。
     */
    @Override
    public IAEItemStack getAvailableItem(@NotNull IAEItemStack request, int iteration) {
        IAEItemStack is = this.getCellItems()
            .findPrecise(request);
        if (is != null) {
            return is.copy();
        }
        return null;
    }

    /**
     * 获取所有可用物品堆栈。
     *
     * @param out       存储可用物品堆栈的输出列表。
     * @param iteration 当前迭代次数，通常用于深度控制或递归限制。
     * @return 返回填充好的输出列表 `out`，包含当前元件中的所有物品堆栈。
     */
    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
        for (final IAEItemStack ais : this.getCellItems()) {
            out.add(ais);
        }
        return out;
    }

    /**
     * 获取磁盘ID
     *
     * @return 磁盘ID的字符串表示，如果没有磁盘ID，则返回空字符串
     */
    @Override
    public String getDiskID() {
        // 检查NBT中是否没有任何标签，如果是，则返回空字符串
        if (this.tagCompound.hasNoTags()) {
            return "";
        }
        // 从NBT中获取磁盘ID的字符串值并返回
        return this.tagCompound.getString(NBTConstants.DISK_ID);
    }

    /**
     * 获取当前元件中的物品列表。
     * <p>
     * 该方法首先检查当前元件是否已经加载了物品列表。如果物品列表尚未加载（即 `cellItems` 为 `null`），
     * 则会调用 `loadCellItems()` 方法来加载物品列表。最后返回元件中的物品列表。
     *
     * @return 返回一个物品列表，包含当前元件中所有物品的堆栈信息。
     */
    private IItemList<IAEItemStack> getCellItems() {
        if (this.cellItems == null) {
            this.loadCellItems();
        }

        return this.cellItems;
    }

    /**
     * 判断物品是否为有效的元件。
     *
     * @param itemStack 要检查的物品堆栈
     * @return 如果物品堆栈是有效的元件，返回 true；否则返回 false
     */
    private static boolean isStorageCell(final IAEItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        try {
            // 检查物品是否为 IStorageCell 类型，并且是否可以存储其他物品
            if (itemStack.getItem() instanceof IStorageCell type) {
                return !type.storableInStorageCell();
            }
        } catch (final Throwable err) {
            return true;
        }

        return false;
    }

    /**
     * 检查给定的物品元件是否为空。
     *
     * @param meInventory 物品元件的库存实例
     * @return 如果物品元件为空，返回 true；否则返回 false
     */
    private boolean isEmpty(final IMEInventory<IAEItemStack> meInventory) {
        boolean isEmpty = meInventory.getAvailableItems(
            AEApi.instance()
                .storage()
                .createItemList(),
            IterationCounter.incrementGlobalDepth())
            .isEmpty();

        IterationCounter.decrementGlobalDepth();

        return isEmpty;
    }

    /**
     * 从元件加载物品列表。
     */
    private void loadCellItems() {
        // 如果物品列表为空，则创建一个新列表
        if (this.cellItems == null) {
            this.cellItems = this.storage.getItems();
            for (IAEItemStack ais : this.cellItems) {
                if (ais != null && ais.getStackSize() <= 0) {
                    ais.reset();
                }
            }
        }

        // 更新物品类型数量
        this.updateItemTypes();

        if (!this.getDiskID()
            .equals(this.storage.getDiskID())) {
            tagCompound.setString(NBTConstants.DISK_ID, this.storage.getDiskID());
        }
    }

    /**
     * 保存物品更改。
     */
    private void saveChanges() {

        // 更新物品类型数量
        this.updateItemTypes();

        if (this.cellItems != null) {
            this.container.saveChanges(this);
        }
        CellItemSavedData.getInstance()
            .markDirty();
    }

    private void updateItemTypes() {
        this.storedItemTypes = this.cellItems.size();
        if (this.cellItems.isEmpty()) {
            this.tagCompound.removeTag(ITEM_TYPE_TAG);
        } else {
            this.tagCompound.setInteger(ITEM_TYPE_TAG, this.storedItemTypes);
        }
    }
}
