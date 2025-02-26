package moe.takochan.takotech.common.data;

import net.minecraft.nbt.NBTTagCompound;

public class WebControllerData {

    public static final String CONTROLLER_ID = "controller_id";

    private String controllerId;

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setString(CONTROLLER_ID, controllerId);
    }

    public void readFormNBT(NBTTagCompound nbt) {
        if (nbt != null) {
            controllerId = nbt.getString(CONTROLLER_ID);
        }
    }
}
