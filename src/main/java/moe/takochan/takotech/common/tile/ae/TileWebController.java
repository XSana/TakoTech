package moe.takochan.takotech.common.tile.ae;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import moe.takochan.takotech.common.data.WebControllerData;
import moe.takochan.takotech.common.tile.BaseAETile;
import moe.takochan.takotech.constants.NBTConstants;

public class TileWebController extends BaseAETile {

    private final WebControllerData data;

    public TileWebController() {
        this.getProxy()
            .setIdlePowerUsage(0);

        this.data = new WebControllerData();
    }

    @Override
    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_AENetwork(final NBTTagCompound data) {
        super.readFromNBT_AENetwork(data);
        NBTTagCompound tag = data.getCompoundTag(NBTConstants.CONTROLLER_DATA);
        if (tag != null) {
            this.getData()
                .readFormNBT(tag);
        }
    }

    @Override
    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_AENetwork(final NBTTagCompound data) {
        super.writeToNBT_AENetwork(data);

        NBTTagCompound dataTag = new NBTTagCompound();
        this.getData()
            .writeToNBT(dataTag);
        data.setTag(NBTConstants.CONTROLLER_DATA, dataTag);
    }

    @Override
    public void onReady() {
        super.onReady();
        if (this.getData()
            .getControllerId() == null || this.getData()
                .getControllerId()
                .isEmpty()) {
            this.getData()
                .setControllerId(
                    UUID.randomUUID()
                        .toString());
            this.markDirty();
        }
    }

    public WebControllerData getData() {
        return data;
    }

}
