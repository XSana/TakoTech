package moe.takochan.takotech.common.data;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;

public class CellItemStorageData {

    // 硬盘Id
    private final String diskID;
    // 物品列表
    private IItemList<IAEItemStack> items;

    public CellItemStorageData(String diskID) {
        this.diskID = diskID;
    }

    /**
     * 通过NBT及diskId获取元件存储实例
     *
     * @param diskId 存储元件的diskId
     * @param data   NBT数据
     * @return 读取的存储实例
     */
    public static CellItemStorageData readFromNBT(String diskId, NBTTagList data) {
        CellItemStorageData storage = new CellItemStorageData(diskId);
        storage.readFromNBT(data);
        return storage;
    }

    /**
     * 获取存储元件的物品列表，若没有则创建一个新的列表。
     *
     * @return 物品列表
     */
    public IItemList<IAEItemStack> getItems() {
        if (this.items == null) {
            this.items = AEApi.instance()
                .storage()
                .createPrimitiveItemList();
        }
        return items;
    }

    /**
     * 判断存储元件是否为空。
     *
     * @return 如果存储元件内没有物品，则返回true
     */
    public boolean isEmpty() {
        return this.getItems()
            .isEmpty();
    }

    /**
     * 获取存储元件的ID。
     *
     * @return 存储元件的ID
     */
    public String getDiskID() {
        return this.diskID;
    }

    /**
     * 从NBT数据中读取元件存储实例
     *
     * @param data NBT数据
     */
    public void readFromNBT(NBTTagList data) {
        for (final IAEItemStack ais : this.readList(data)) {
            this.getItems()
                .add(ais);
        }
    }

    /**
     * 将存储元件数据写入NBT。
     *
     * @return 存储元件的NBT数据
     */
    public NBTBase writeToNBT() {
        return writeList(this.getItems());
    }

    /**
     * 从NBT列表读取物品。
     *
     * @param tag 存储物品的NBT列表
     * @return 读取的物品列表
     */
    private IItemList<IAEItemStack> readList(final NBTTagList tag) {
        final IItemList<IAEItemStack> out = AEApi.instance()
            .storage()
            .createItemList();

        if (tag == null) {
            return out;
        }

        for (int x = 0; x < tag.tagCount(); x++) {
            final IAEItemStack ais = AEItemStack.loadItemStackFromNBT(tag.getCompoundTagAt(x));
            if (ais != null) {
                out.add(ais);
            }
        }

        return out;
    }

    /**
     * 将物品列表写入NBT。
     *
     * @param myList 要写入的物品列表
     * @return 存储物品的NBT列表
     */
    private NBTTagList writeList(final IItemList<IAEItemStack> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEItemStack ais : myList) {
            if (ais.getStackSize() > 0) {
                out.appendTag(this.writeItem(ais));
            }
        }

        return out;
    }

    /**
     * 将单个物品写入NBT。
     *
     * @param item 物品实例
     * @return 存储物品信息的NBT数据
     */
    private NBTTagCompound writeItem(final IAEItemStack item) {
        final NBTTagCompound out = new NBTTagCompound();

        if (item != null) {
            item.writeToNBT(out);
        }

        return out;
    }
}
