package moe.takochan.takotech.common.data;

import net.minecraft.nbt.NBTTagCompound;

import moe.takochan.takotech.constants.NBTConstants;

public class WebControllerData {

    private String controllerId;

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setString(NBTConstants.CONTROLLER_ID, controllerId);
    }

    public void readFormNBT(NBTTagCompound nbt) {
        controllerId = nbt.getString(NBTConstants.CONTROLLER_ID);
    }
}
