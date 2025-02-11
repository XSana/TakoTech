package moe.takochan.takotech.common.storage;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import moe.takochan.takotech.common.Reference;
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

public class StorageCellSaveData extends WorldSavedData {

    private static StorageCellSaveData INSTANCE; // **全局单例**
    private final Map<String, IAEItemStack[]> storedItems = new HashMap<>();

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
        storedItems.clear();
        NBTTagList list = nbt.getTagList(NBTConstants.DISK_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String diskID = tag.getString(NBTConstants.DISK_ID);
            NBTTagList items = tag.getTagList(NBTConstants.DISK_ITEMS, Constants.NBT.TAG_COMPOUND);

            IAEItemStack[] itemStacks = new IAEItemStack[items.tagCount()];
            for (int j = 0; j < items.tagCount(); j++) {
                NBTTagCompound itemTag = items.getCompoundTagAt(j);
                itemStacks[j] = AEApi.instance().storage().createItemStack(ItemStack.loadItemStackFromNBT(itemTag));
            }
            storedItems.put(diskID, itemStacks);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, IAEItemStack[]> entry : storedItems.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }

            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(NBTConstants.DISK_ID, entry.getKey());

            NBTTagList items = new NBTTagList();
            for (IAEItemStack stack : entry.getValue()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                stack.getItemStack().writeToNBT(itemTag);
                items.appendTag(itemTag);
            }
            tag.setTag(NBTConstants.DISK_ITEMS, items);
            list.appendTag(tag);
        }
        nbt.setTag(NBTConstants.DISK_LIST, list);
    }

    public void setStoredItems(String diskID, IItemList<IAEItemStack> items) {
        storedItems.put(diskID, items.toArray(new IAEItemStack[0]));
    }

    public IAEItemStack[] getStoredItems(String diskID) {
        return storedItems.getOrDefault(diskID, new IAEItemStack[0]);
    }
}
