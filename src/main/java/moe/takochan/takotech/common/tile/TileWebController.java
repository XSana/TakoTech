package moe.takochan.takotech.common.tile;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import moe.takochan.takotech.constants.NBTConstants;

public class TileWebController extends BaseAETile {

    private String controllerId;

    public TileWebController() {
        this.getProxy()
            .setIdlePowerUsage(0);
    }

    @Override
    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_AENetwork(final NBTTagCompound data) {
        super.readFromNBT_AENetwork(data);
        this.setControllerId(data.getString(NBTConstants.CONTROLLER_ID));
    }

    @Override
    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_AENetwork(final NBTTagCompound data) {
        super.writeToNBT_AENetwork(data);
        data.setString(NBTConstants.CONTROLLER_ID, this.getControllerId());
    }

    @Override
    public void onReady() {
        super.onReady();
        if (getControllerId() == null || getControllerId().isEmpty()) {
            this.setControllerId(
                UUID.randomUUID()
                    .toString());
            this.markDirty();
        }
    }

    public String getControllerId() {
        return this.controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }
}
