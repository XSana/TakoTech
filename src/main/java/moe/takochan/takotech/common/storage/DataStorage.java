package moe.takochan.takotech.common.storage;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.UUID;

public class DataStorage {

    private IItemList<IAEItemStack> items;

    private final UUID uuid;

    public DataStorage(UUID uuid) {
        this.uuid = uuid;
    }

    public IItemList<IAEItemStack> getItems() {
        if (this.items == null) {
            this.items = AEApi.instance()
                .storage()
                .createPrimitiveItemList();
        }
        return items;
    }

    public boolean isEmpty() {
        return this.getItems().isEmpty();
    }

    public String getUUID() {
        return this.uuid.toString();
    }

    public UUID getRawUUID() {
        return this.uuid;
    }

    public static DataStorage readFromNBT(UUID uuid, NBTTagList data) {
        DataStorage storage = new DataStorage(uuid);
        storage.readFromNBT(data);
        return storage;
    }

    public void readFromNBT(NBTTagList data) {
        for (final IAEItemStack ais : this.readList(data)) {
            this.getItems()
                .add(ais);
        }
    }

    public NBTBase writeToNBT() {
        return writeList(this.getItems());
    }


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

    private NBTTagList writeList(final IItemList<IAEItemStack> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEItemStack ais : myList) {
            if (ais.getStackSize() > 0) {
                out.appendTag(this.writeItem(ais));
            }
        }

        return out;
    }

    private NBTTagCompound writeItem(final IAEItemStack item) {
        final NBTTagCompound out = new NBTTagCompound();

        if (item != null) {
            item.writeToNBT(out);
        }

        return out;
    }
}
