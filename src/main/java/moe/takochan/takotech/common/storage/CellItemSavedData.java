package moe.takochan.takotech.common.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import appeng.util.Platform;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.BaseAECellItem;
import moe.takochan.takotech.constants.NBTConstants;

/**
 * `StorageComponentSavedData` 类负责管理存储元件数据。
 * 它继承自 `WorldSavedData`，用于保存特定于世界的存储元件数据。
 * 这些数据是持久化的，可以跨世界加载和保存。
 */
public class CellItemSavedData extends WorldSavedData {

    private final static String DATA_NAME = Reference.MODID + "_Cell";

    private static CellItemSavedData INSTANCE;
    private final Map<String, CellItemStorage> disks = new HashMap<>();

    public CellItemSavedData() {
        this(DATA_NAME);
    }

    public CellItemSavedData(String name) {
        super(name);
    }

    /**
     * 初始化 `StorageComponentSavedData`，将其加载到指定世界的地图存储中。
     * 如果没有现有的数据，则创建新的实例并存储。
     * 这个方法是同步的，确保在一个世界加载时只会初始化一次。
     *
     * @param world 当前加载的世界
     */
    public static synchronized void init(World world) {
        // 设置为null以保证触发垃圾回收机制回收掉旧实例
        INSTANCE = null;

        MapStorage storage = world.mapStorage;
        CellItemSavedData data = (CellItemSavedData) storage.loadData(CellItemSavedData.class, DATA_NAME);
        if (data == null) {
            data = new CellItemSavedData();
            storage.setData(Reference.MODID, data);
        }
        INSTANCE = data;
    }

    /**
     * @return `StorageComponentSavedData` 单例
     */
    public static CellItemSavedData getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                "StorageCellData has not been initialized! Ensure it is initialized during WorldLoad.");
        }
        return INSTANCE;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        Map<String, CellItemStorage> n = new HashMap<>();
        NBTTagList list = nbt.getTagList(NBTConstants.DISK_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String diskID = tag.getString(NBTConstants.DISK_ID);
            NBTTagList items = tag.getTagList(NBTConstants.DISK_ITEMS, Constants.NBT.TAG_COMPOUND);
            CellItemStorage storage = CellItemStorage.readFromNBT(diskID, items);
            n.put(diskID, storage);
        }
        disks.clear();
        disks.putAll(n);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, CellItemStorage> entry : disks.entrySet()) {
            if (entry.getValue() == null || entry.getValue()
                .isEmpty()) {
                continue;
            }

            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(NBTConstants.DISK_ID, entry.getKey());
            tag.setTag(
                NBTConstants.DISK_ITEMS,
                entry.getValue()
                    .writeToNBT());
            list.appendTag(tag);
        }
        nbt.setTag(NBTConstants.DISK_LIST, list);
    }

    /**
     * 获取与指定存储元件项堆栈相关的数据存储。
     *
     * @param itemStack 物品堆栈
     * @return 与存储元件项堆栈关联的 `CellItemStorage` 数据
     */
    public CellItemStorage getDataStorage(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BaseAECellItem) {
            NBTTagCompound tag = Platform.openNbtData(itemStack);
            String diskId = tag.getString(NBTConstants.DISK_ID);
            if (diskId == null || diskId.isEmpty()) {
                diskId = UUID.randomUUID()
                    .toString();
            }
            return disks.computeIfAbsent(diskId, CellItemStorage::new);
        }
        return null;
    }
}
