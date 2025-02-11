package moe.takochan.takotech.common.storage;

import appeng.util.Platform;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.BaseAECellItem;
import moe.takochan.takotech.constants.NBTConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageCellSaveData extends WorldSavedData {

    private static StorageCellSaveData INSTANCE;
    private final Map<UUID, DataStorage> disks = new HashMap<>();

    public StorageCellSaveData() {
        this(Reference.MODID);
    }

    public StorageCellSaveData(String name) {
        super(name);
    }

    public static synchronized void init(World world) {
        if (INSTANCE == null) {
            MapStorage storage = world.mapStorage;
            StorageCellSaveData data = (StorageCellSaveData) storage.loadData(StorageCellSaveData.class, Reference.MODID);
            if (data == null) {
                data = new StorageCellSaveData();
                storage.setData(Reference.MODID, data);
            }
            INSTANCE = data;
        }
    }

    /**
     * 获取全局 `StorageCellData`
     *
     * @return `StorageCellData` 单例
     */
    public static StorageCellSaveData getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("StorageCellData has not been initialized! Ensure it is initialized during WorldLoad.");
        }
        return INSTANCE;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        Map<UUID, DataStorage> n = new HashMap<>();
        NBTTagList list = nbt.getTagList(NBTConstants.DISK_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            UUID diskID = UUID.fromString(tag.getString(NBTConstants.DISK_ID));
            NBTTagList items = tag.getTagList(NBTConstants.DISK_ITEMS, Constants.NBT.TAG_COMPOUND);
            DataStorage storage = DataStorage.readFromNBT(diskID, items);
            n.put(diskID, storage);
        }
        disks.clear();
        disks.putAll(n);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, DataStorage> entry : disks.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(NBTConstants.DISK_ID, entry.getKey().toString());
            tag.setTag(NBTConstants.DISK_ITEMS, entry.getValue().writeToNBT());
            list.appendTag(tag);
        }
        nbt.setTag(NBTConstants.DISK_LIST, list);
    }

    public DataStorage getDataStorage(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BaseAECellItem) {
            NBTTagCompound tag = Platform.openNbtData(itemStack);
            String diskId = tag.getString(NBTConstants.DISK_ID);
            if (diskId == null || diskId.isEmpty()) {
                diskId = UUID.randomUUID().toString();
            }
            UUID uuid = UUID.fromString(diskId);
            return disks.computeIfAbsent(uuid, DataStorage::new);
        }
        return null;
    }
}
